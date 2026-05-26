import React, { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { Card, CardBody, CardHeader, CardFooter } from '../../components/common/Card';
import { useAuth } from '../../hooks/useAuth';
import { validatePassword } from '../../utils/validators';

interface FormErrors {
  usernameOrEmail?: string;
  password?: string;
}

export const Login: React.FC = () => {
  const { login, isAuthenticated } = useAuth();
  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<FormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  const validate = (): boolean => {
    const newErrors: FormErrors = {
      usernameOrEmail: usernameOrEmail.trim() ? undefined : 'Username or email is required',
      password: validatePassword(password) || undefined,
    };
    setErrors(newErrors);
    return !Object.values(newErrors).some(Boolean);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitError(null);

    if (!validate()) {
      return;
    }

    setIsSubmitting(true);

    try {
      await login(usernameOrEmail, password);
    } catch (error) {
      setSubmitError(
        error instanceof Error ? error.message : 'Login failed. Please try again.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center px-4 py-12">
      <Card className="w-full max-w-md">
        <CardHeader>
          <h1 className="text-xl font-semibold tracking-wide text-neutral-100">
            Sign In
          </h1>
          <p className="mt-2 text-sm text-neutral-400">
            Welcome back! Please enter your credentials.
          </p>
        </CardHeader>
        <CardBody>
          <form onSubmit={handleSubmit} className="space-y-5">
            {submitError && (
              <div className="p-3 text-sm text-red-400 bg-red-900/20 border border-red-800/50 rounded-sm">
                {submitError}
              </div>
            )}
            <Input
              id="usernameOrEmail"
              label="Username or email"
              type="text"
              autoComplete="username"
              value={usernameOrEmail}
              onChange={(e) => setUsernameOrEmail(e.target.value)}
              error={errors.usernameOrEmail}
              placeholder="username or email@example.com"
            />
            <Input
              id="password"
              label="Password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              error={errors.password}
              placeholder="Enter your password"
            />
            <Button
              type="submit"
              variant="primary"
              isLoading={isSubmitting}
              className="w-full"
            >
              Sign In
            </Button>
          </form>
        </CardBody>
        <CardFooter>
          <p className="text-sm text-neutral-400 text-center">
            Don't have an account?{' '}
            <Link
              to="/register"
              className="text-amber-500 hover:text-amber-400 transition-colors"
            >
              Create one
            </Link>
          </p>
        </CardFooter>
      </Card>
    </div>
  );
};
