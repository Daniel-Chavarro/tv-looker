import { useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { useList, useUpdateList, useDeleteList, useRemoveItemFromList } from '../hooks/useLists';
import { PageLoader } from '../components/common/Loader';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Modal } from '../components/common/Modal';
import { ItemGrid } from '../components/features/ItemGrid';

export function ListDetail() {
  const { id } = useParams<{ id: string }>();
  const listId = Number(id);
  const navigate = useNavigate();

  const { data, isLoading, error, refetch } = useList(listId);
  const updateList = useUpdateList();
  const deleteList = useDeleteList();
  const removeItem = useRemoveItemFromList();

  const [showEditModal, setShowEditModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [updateError, setUpdateError] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [removeItemError, setRemoveItemError] = useState<string | null>(null);

  const handleEdit = () => {
    if (!data) return;
    setUpdateError(null);
    setName(data.name);
    setDescription(data.description || '');
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

  const handleUpdateList = async () => {
    if (!name.trim()) return;
    setUpdateError(null);
    try {
      await updateList.mutateAsync({
        id: listId,
        data: {
          name: name.trim(),
          description: description.trim() || undefined,
        },
      });
      closeEditModal();
    } catch (err) {
      setUpdateError('Failed to update list. Please try again.');
      console.error('Failed to update list:', err);
    }
  };

  const handleDeleteList = async () => {
    setDeleteError(null);
    try {
      await deleteList.mutateAsync(listId);
      navigate('/lists');
    } catch (err) {
      setDeleteError('Failed to delete list. Please try again.');
      console.error('Failed to delete list:', err);
    }
  };

  const handleRemoveItem = async (itemId: number) => {
    if (!confirm('Remove this item from the list?')) return;
    setRemoveItemError(null);
    try {
      await removeItem.mutateAsync({ listId, itemId });
    } catch (err) {
      setRemoveItemError('Failed to remove item from list. Please try again.');
      console.error('Failed to remove item:', err);
    }
  };

  if (isLoading) {
    return <PageLoader message="Loading list..." />;
  }

  if (error || !data) {
    return (
      <ErrorMessage
        message="Failed to load list. Please try again."
        retry={() => refetch()}
      />
    );
  }

  const list = data;
  const items = list.items;

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold text-neutral-100 tracking-tight mb-2">
            {list.name}
          </h1>
          {list.description && (
            <p className="text-neutral-500">{list.description}</p>
          )}
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={handleEdit}>
            Edit
          </Button>
          <Button variant="danger" onClick={openDeleteModal}>
            Delete
          </Button>
        </div>
      </div>

      {removeItemError && (
        <div
          role="alert"
          className="mb-6 rounded-sm border border-red-900/30 bg-red-950/20 px-4 py-3 text-sm text-red-300"
        >
          {removeItemError}
        </div>
      )}

      {items.length > 0 ? (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {items.map((item) => (
            <div key={item.id} className="relative group">
              <Link to={`/items/${item.id}`} className="block">
                <div className="aspect-[2/3] rounded-sm overflow-hidden bg-neutral-900">
                  {item.posterUrl ? (
                    <img
                      src={item.posterUrl}
                      alt={item.title}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center">
                      <span className="text-neutral-700 text-4xl">?</span>
                    </div>
                  )}
                </div>
                <p className="text-neutral-200 text-sm mt-2 line-clamp-2">
                  {item.title}
                </p>
              </Link>
              <button
                aria-label={`Remove ${item.title} from list`}
                onClick={() => handleRemoveItem(item.id)}
                className="absolute top-2 right-2 p-2 bg-neutral-900/80 rounded-full opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-900/80"
              >
                <svg
                  className="w-4 h-4 text-neutral-100"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            </div>
          ))}
        </div>
      ) : (
        <div className="text-center py-16">
          <p className="text-neutral-500 mb-4">This list is empty.</p>
          <Link
            to="/"
            className="inline-flex items-center justify-center px-6 py-3 rounded-sm text-sm tracking-wide uppercase transition-all duration-300 ease-out focus:outline-none focus:ring-2 focus:ring-amber-500/50 focus:ring-offset-2 focus:ring-offset-neutral-900 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:transform-none hover:transform hover:-translate-y-0.5 bg-amber-500 hover:bg-amber-400 text-neutral-900 font-semibold shadow-lg shadow-amber-500/20"
          >
            Browse Items
          </Link>
        </div>
      )}

      <Modal isOpen={showEditModal} onClose={closeEditModal} title="Edit List">
        <div className="space-y-4">
          <Input
            label="List Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Enter list name"
          />
          <Input
            label="Description (optional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Enter description"
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
              onClick={handleUpdateList}
              disabled={!name.trim() || updateList.isPending}
              isLoading={updateList.isPending}
              className="flex-1"
            >
              Save
            </Button>
          </div>
        </div>
      </Modal>

      <Modal isOpen={showDeleteModal} onClose={closeDeleteModal} title="Delete List">
        <div className="space-y-4">
          <p className="text-neutral-300">
            Are you sure you want to delete "{list.name}"? This action cannot be undone.
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
              onClick={handleDeleteList}
              isLoading={deleteList.isPending}
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
