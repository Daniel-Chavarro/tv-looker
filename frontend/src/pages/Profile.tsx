import { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useUser, useUpdateUser, useDeleteUser } from '../hooks/useUsers';
import { PageLoader } from '../components/common/Loader';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Modal } from '../components/common/Modal';

export function Profile() {
  const { user: authUser, logout } = useAuth();
  const { data, isLoading, error, refetch } = useUser(authUser?.id ?? '');
  const updateUser = useUpdateUser();
  const deleteUser = useDeleteUser();

  const [showEditModal, setShowEditModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [updateError, setUpdateError] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const handleEdit = () => {
    if (!data) return;
    setUpdateError(null);
    setName(data.name || '');
    setEmail(data.email);
    setShowEditModal(true);
  };

  const closeEditModal = () => {
    setUpdateError(null);
    setShowEditModal(false);
  };

  const openDeleteModal = () => {
    setDeleteError(null);
    setShowDeleteModal(true);
  };

  const closeDeleteModal = () => {
    setDeleteError(null);
    setShowDeleteModal(false);
  };

  const handleUpdateProfile = async () => {
    if (!authUser?.id) return;
    setUpdateError(null);
    try {
      await updateUser.mutateAsync({
        id: authUser.id,
        data: {
          name: name.trim() || undefined,
          email: email.trim() || undefined,
        },
      });
      closeEditModal();
    } catch (err) {
      setUpdateError('Failed to update profile. Please try again.');
      console.error('Failed to update profile:', err);
    }
  };

  const handleDeleteProfile = async () => {
    if (!authUser?.id) return;
    setDeleteError(null);
    try {
      await deleteUser.mutateAsync(authUser.id);
      logout();
    } catch (err) {
      setDeleteError('Failed to delete profile. Please try again.');
      console.error('Failed to delete profile:', err);
    }
  };

  if (isLoading) {
    return <PageLoader message="Loading profile..." />;
  }

  if (error || !data) {
    return (
      <ErrorMessage
        message="Failed to load profile. Please try again."
        retry={() => refetch()}
      />
    );
  }

  const user = data;
  const joinDate = new Date(user.createdAt).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="max-w-2xl mx-auto">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl font-bold text-neutral-100 tracking-tight">
            Profile
          </h1>
          <Button variant="secondary" onClick={handleEdit}>
            Edit Profile
          </Button>
        </div>

        <div className="bg-neutral-900/40 border border-neutral-800 rounded-sm p-6">
          <div className="flex items-center gap-6 mb-8">
            <div className="w-20 h-20 rounded-full bg-neutral-800 flex items-center justify-center">
              <span className="text-amber-500 text-3xl font-bold">
                {user.username[0].toUpperCase()}
              </span>
            </div>
            <div>
              <h2 className="text-2xl font-semibold text-neutral-100">
                {user.name || user.username}
              </h2>
              <p className="text-neutral-500">@{user.username}</p>
            </div>
          </div>

          <div className="space-y-6">
            <div>
              <label className="block text-sm text-neutral-500 tracking-wider uppercase mb-1">
                Username
              </label>
              <p className="text-neutral-200">{user.username}</p>
            </div>

            <div>
              <label className="block text-sm text-neutral-500 tracking-wider uppercase mb-1">
                Email
              </label>
              <p className="text-neutral-200">{user.email}</p>
            </div>

            {user.name && (
              <div>
                <label className="block text-sm text-neutral-500 tracking-wider uppercase mb-1">
                  Name
                </label>
                <p className="text-neutral-200">{user.name}</p>
              </div>
            )}

            <div>
              <label className="block text-sm text-neutral-500 tracking-wider uppercase mb-1">
                Member Since
              </label>
              <p className="text-neutral-200">{joinDate}</p>
            </div>
          </div>
        </div>

        <div className="mt-8">
          <Button
            variant="ghost"
            onClick={() => logout()}
            className="text-neutral-400"
          >
            Sign Out
          </Button>
        </div>

        <div className="mt-8 pt-8 border-t border-neutral-800">
          <Button variant="danger" onClick={openDeleteModal}>
            Delete Account
          </Button>
          <p className="text-neutral-600 text-sm mt-2">
            This action cannot be undone. All your data will be permanently deleted.
          </p>
        </div>
      </div>

      <Modal isOpen={showEditModal} onClose={closeEditModal} title="Edit Profile">
        <div className="space-y-4">
          <Input
            label="Name (optional)"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Enter your name"
          />
          <Input
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Enter your email"
          />
          {updateError && (
            <div
              role="alert"
              className="rounded-sm border border-red-900/30 bg-red-950/20 px-4 py-3 text-sm text-red-300"
            >
              {updateError}
            </div>
          )}
          <div className="flex gap-3 pt-4">
            <Button
              variant="secondary"
              onClick={closeEditModal}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              onClick={handleUpdateProfile}
              disabled={!email.trim() || updateUser.isPending}
              isLoading={updateUser.isPending}
              className="flex-1"
            >
              Save
            </Button>
          </div>
        </div>
      </Modal>

      <Modal isOpen={showDeleteModal} onClose={closeDeleteModal} title="Delete Account">
        <div className="space-y-4">
          <p className="text-neutral-300">
            Are you sure you want to delete your account? This action cannot be
            undone.
          </p>
          {deleteError && (
            <div
              role="alert"
              className="rounded-sm border border-red-900/30 bg-red-950/20 px-4 py-3 text-sm text-red-300"
            >
              {deleteError}
            </div>
          )}
          <div className="flex gap-3 pt-4">
            <Button
              variant="secondary"
              onClick={closeDeleteModal}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              onClick={handleDeleteProfile}
              isLoading={deleteUser.isPending}
              className="flex-1"
            >
              Delete
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
