import { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useCurrentUser, useUpdateCurrentUser } from '../hooks/useUsers';
import { PageLoader } from '../components/common/Loader';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Modal } from '../components/common/Modal';

export function Profile() {
  const { logout } = useAuth();
  const { data, isLoading, error, refetch } = useCurrentUser();
  const updateCurrentUser = useUpdateCurrentUser();

  const [showEditModal, setShowEditModal] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [updateError, setUpdateError] = useState<string | null>(null);

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

  const handleUpdateProfile = async () => {
    setUpdateError(null);
    try {
      await updateCurrentUser.mutateAsync({
        name: name.trim() || undefined,
        email: email.trim() || undefined,
      });
      closeEditModal();
    } catch (err) {
      setUpdateError('Failed to update profile. Please try again.');
      console.error('Failed to update profile:', err);
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
              disabled={!email.trim() || updateCurrentUser.isPending}
              isLoading={updateCurrentUser.isPending}
              className="flex-1"
            >
              Save
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
