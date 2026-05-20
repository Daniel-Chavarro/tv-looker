#!/usr/bin/env python3
"""
Calculator for package instability metrics (Ca, Ce, I).

Reads Java source code, extracts intra-project dependencies from import
statements, and computes afferent coupling (Ca), efferent coupling (Ce),
and instability (I = Ce / (Ca + Ce)) for 6 conceptual packages.

Usage:
    python scripts/instability_metrics.py

Output: Markdown table + LaTeX fragment to stdout.
"""

import os
import re
import sys
from collections import OrderedDict, defaultdict

# ============================================================
# CONFIGURATION: Map of conceptual package names to internal
# fully-qualified name (FQN) patterns.
#
# Order matters: more specific patterns must appear BEFORE
# more general ones (e.g. "service.tmdb." before "service.").
# ============================================================

PACKAGE_MAP = OrderedDict([
    ("security", [
        "config.SecurityConfig",
        "service.AuthService",
        "service.JwtService",
    ]),
    ("integration/tmdb", [
        "service.tmdb.",
        "persistence.tmdb.",
        "infrastructure.tmdb.",
        "config.TmdbConfig",
    ]),
    ("recommendation", [
        "domain.motor.",
        "domain.strategy.",
        "config.RecommendationConfig",
    ]),
    ("domain.model", [
        "domain.model.",
        "domain.exception.",
        "domain.data_structure.",
    ]),
    ("persistence/repository", [
        "persistence.repository.",
    ]),
    ("application/service", [
        "service.",
    ]),
])


def classify(fqn):
    """
    Map a fully-qualified type name to a conceptual package.

    Parameters
    ----------
    fqn : str
        Fully-qualified name, e.g. "org.tvl.tvlooker.domain.model.dto.User"
        or "org.tvl.tvlooker.service.AuthService".

    Returns
    -------
    str or None
        Conceptual package name (e.g. "domain.model"), or None if the FQN
        does not belong to any of the tracked packages (external library,
        excluded internal package, etc.).
    """
    if not fqn.startswith("org.tvl.tvlooker."):
        return None

    inner = fqn[len("org.tvl.tvlooker."):]

    for pkg_name, patterns in PACKAGE_MAP.items():
        for pattern in patterns:
            if pattern.endswith("."):
                # Prefix match (e.g. "service.tmdb." matches "service.tmdb.TmdbDataFetcher")
                if inner.startswith(pattern):
                    return pkg_name
            else:
                # Exact match (e.g. "config.SecurityConfig") or FQN prefix
                # (e.g. "config.SecurityConfig" also matches as standalone)
                if inner == pattern or inner.startswith(pattern + "."):
                    return pkg_name

    return None


# Regex to match Java import statements:
#   import org.tvl.tvlooker.some.package.ClassName;
IMPORT_RE = re.compile(
    r'^\s*import\s+'
    r'(static\s+)?'                     # optional 'static'
    r'([a-zA-Z_][\w.]*(?:\.[A-Z_][\w]*)+)'
    r'\s*;'
)


def extract_imports(filepath):
    """
    Extract all import FQNs from a Java source file.

    Parameters
    ----------
    filepath : str
        Absolute path to a .java file.

    Returns
    -------
    list of str
        Fully-qualified names of all imported types.
    """
    imports = []
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            for line in f:
                m = IMPORT_RE.match(line)
                if m:
                    fqn = m.group(2)
                    imports.append(fqn)
    except (OSError, UnicodeDecodeError) as e:
        print(f"Warning: could not read {filepath}: {e}", file=sys.stderr)
    return imports


def get_fqn_from_path(filepath, src_root):
    """
    Derive the fully-qualified class name from a file's path.

    Example:
        path = ".../src/main/java/org/tvl/tvlooker/service/AuthService.java"
        src_root = ".../src/main/java"
        returns "org.tvl.tvlooker.service.AuthService"

    Parameters
    ----------
    filepath : str
        Absolute path to a .java file.
    src_root : str
        Absolute path to the Java source root (src/main/java).

    Returns
    -------
    str
        Fully-qualified type name.
    """
    rel = os.path.relpath(filepath, src_root)
    fqn = rel.replace(os.sep, '.')[:-5]  # strip '.java'
    return fqn


# ============================================================
# Interpretations
# ============================================================

def interpret_instability(i_str):
    """Return a human-readable interpretation for a given I value."""
    if i_str == "N/A":
        return "Aislado - sin acoplamiento con otros paquetes."
    i = float(i_str)
    if i <= 0.15:
        return ("Muy estable - fuertemente dependido por otros; "
                "cambios requieren coordinacion.")
    elif i <= 0.35:
        return ("Estable - pocas dependencias salientes, "
                "varios paquetes dependen de el.")
    elif i <= 0.55:
        return ("Moderado - equilibrio entre depender y ser dependido.")
    elif i <= 0.75:
        return ("Inestable - mas dependiente que dependido; "
                "cambios internos tienen bajo impacto externo.")
    else:
        return ("Muy inestable - fuertemente dependiente de otros; "
                "facil de modificar sin afectar al sistema.")


# ============================================================
# Main
# ============================================================

def main():
    # Locate the source root (src/main/java) relative to the script.
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    src_root = os.path.join(project_root, "src", "main", "java")

    if not os.path.isdir(src_root):
        print(f"Error: Java source root not found at {src_root}",
              file=sys.stderr)
        sys.exit(1)

    # ----------------------------------------------------------
    # Step 1: Discover all Java files belonging to the 6 packages
    # ----------------------------------------------------------
    pkg_files = defaultdict(list)

    for root, dirs, files in os.walk(src_root):
        for f in files:
            if not f.endswith('.java'):
                continue
            filepath = os.path.join(root, f)
            fqn = get_fqn_from_path(filepath, src_root)
            pkg = classify(fqn)
            if pkg:
                pkg_files[pkg].append(filepath)

    all_pkgs_found = set(pkg_files.keys())
    expected_pkgs = set(PACKAGE_MAP.keys())

    # Warn if any expected package has zero files.
    for pkg in expected_pkgs:
        if pkg not in all_pkgs_found:
            print(f"Info: No source files found for package '{pkg}'.",
                  file=sys.stderr)
        else:
            print(f"Found {len(pkg_files[pkg])} files in '{pkg}'.",
                  file=sys.stderr)

    # ----------------------------------------------------------
    # Step 2: Build dependency graph
    # ----------------------------------------------------------
    ce_counts = defaultdict(set)  # pkg -> {target packages it depends on}
    ca_counts = defaultdict(set)  # pkg -> {source packages that depend on it}

    for pkg, files in pkg_files.items():
        for filepath in files:
            imports = extract_imports(filepath)
            for imp in imports:
                target_pkg = classify(imp)
                if target_pkg is not None and target_pkg != pkg:
                    ce_counts[pkg].add(target_pkg)
                    ca_counts[target_pkg].add(pkg)

    # ----------------------------------------------------------
    # Step 3: Compute instability for each package
    # ----------------------------------------------------------
    results = []

    for pkg in PACKAGE_MAP:
        ca = len(ca_counts.get(pkg, set()))
        ce = len(ce_counts.get(pkg, set()))

        if ca == 0 and ce == 0:
            i_str = "N/A"
        else:
            i = ce / (ca + ce) if (ca + ce) > 0 else 0.0
            i_str = f"{i:.2f}"

        results.append({
            "package": pkg,
            "ca": ca,
            "ce": ce,
            "i": i_str,
            "interpretation": interpret_instability(i_str),
        })

    # ----------------------------------------------------------
    # Step 4: Output — Markdown table
    # ----------------------------------------------------------
    print()
    print("# Metricas de Inestabilidad por Paquete\n")
    print("| Paquete | Ca | Ce | I | Interpretacion |")
    print("|---|---|---|---|---|")
    for r in results:
        print(f"| {r['package']} | {r['ca']} | {r['ce']} | {r['i']} | {r['interpretation']} |")

    # ----------------------------------------------------------
    # Step 5: Validations
    # ----------------------------------------------------------
    print()
    print("## Validaciones\n")

    domain = next(r for r in results if r['package'] == 'domain.model')
    if domain['i'] != "N/A" and float(domain['i']) > 0.5:
        print(":warning: **ALERTA**: `domain.model` tiene I = "
              f"{domain['i']} (> 0.5).")
        print("   El dominio depende demasiado de infraestructura. "
              "Revise dependencias hacia afuera del modelo.")
    else:
        print(f":white_check_mark: `domain.model` con I = {domain['i']}. "
              "Correctamente estable (reglas de negocio protegidas).")

    security = next(r for r in results if r['package'] == 'security')
    if security['i'] != "N/A" and float(security['i']) > 0.7:
        print(":warning: **ADVERTENCIA**: `security` tiene I = "
              f"{security['i']}. Posible fuga de responsabilidades "
              "o dependencia excesiva.")

    isolated = [r for r in results if r['i'] == "N/A"]
    if isolated:
        for r in isolated:
            print(f":information_source: `{r['package']}` es un paquete "
                  "aislado (Ca=0, Ce=0). Sin interdependencias con otros "
                  "paquetes del sistema.")

    # ----------------------------------------------------------
    # Step 6: Refactoring recommendations
    # ----------------------------------------------------------
    print()
    print("## Recomendaciones de Refactorizacion\n")

    for r in results:
        if r['i'] != "N/A":
            i_val = float(r['i'])
            pkg = r['package']
            if i_val > 0.65:
                print(f"- **{pkg}** (I={r['i']}): Alta inestabilidad. "
                      "Si este paquete contiene reglas de negocio, "
                      "considere estabilizar sus interfaces. "
                      "Si es un adaptador externo, el valor puede ser aceptable.")
            elif i_val < 0.20 and r['ca'] > 0:
                print(f"- **{pkg}** (I={r['i']}): Alta estabilidad. "
                      f"Es referenciado por {r['ca']} paquete(s). "
                      "Proteja sus contratos; cambios aqui afectan "
                      "a multiples dependientes.")
            elif i_val < 0.20 and r['ca'] == 0:
                print(f"- **{pkg}** (I={r['i']}): Estable pero no "
                      "dependido. Verificar si es necesario o "
                      "si es codigo muerto.")

    print()

    # ----------------------------------------------------------
    # Step 7: LaTeX fragment
    # ----------------------------------------------------------
    print("---")
    print()
    print("## Codigo LaTeX para el informe\n")
    print("\\begin{longtable}{L{4.4cm}L{1.0cm}L{1.0cm}L{1.0cm}L{5.9cm}}")
    print("\\caption{Metricas de inestabilidad por paquete "
          "(resultados del analisis estatico).}\\\\")
    print("\\toprule")
    print("\\textbf{Paquete} & \\textbf{$C_a$} & \\textbf{$C_e$} "
          "& \\textbf{$I$} & \\textbf{Interpretacion} \\\\")
    print("\\midrule")
    for r in results:
        pkg_latex = r['package'].replace('/', '/')
        interp_latex = r['interpretation']
        print(f"{pkg_latex} & {r['ca']} & {r['ce']} & {r['i']} "
              f"& {interp_latex} \\\\")
    print("\\bottomrule")
    print("\\end{longtable}")
    print()

    # Print dependency details for LaTeX footnotes / technical notes.
    print("% Detalle de dependencias por paquete (opcional)\n")
    for r in results:
        if r['ca'] > 0 or r['ce'] > 0:
            sources = ca_counts.get(r['package'], set())
            targets = ce_counts.get(r['package'], set())
            if sources:
                print(f"% Ca de {r['package']}: {', '.join(sorted(sources))}")
            if targets:
                print(f"% Ce de {r['package']}: {', '.join(sorted(targets))}")
    print()


if __name__ == "__main__":
    main()
