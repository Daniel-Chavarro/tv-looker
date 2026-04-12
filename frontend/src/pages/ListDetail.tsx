import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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

  const handleEdit = () => {
    if (!data?.data) return;
    setName(data.data.name);
    setDescription(data.data.description || '');
    setShowEditModal(true);
  };

  const handleUpdateList = async () => {
    if (!name.trim()) return;
    try {
      await updateList.mutateAsync({
        id: listId,
        data: {
          name: name.trim(),
          description: description.trim() || undefined,
        },
      });
      setShowEditModal(false);
    } catch (err) {
      console.error('Failed to update list:', err);
    }
  };

  const handleDeleteList = async () => {
    try {
      await deleteList.mutateAsync(listId);
      navigate('/lists');
    } catch (err) {
      console.error('Failed to delete list:', err);
    }
  };

  const handleRemoveItem = async (itemId: number) => {
    if (!confirm('Remove this item from the list?')) return;
    try {
      await removeItem.mutateAsync({ listId, itemId });
    } catch (err) {
      console.error('Failed to remove item:', err);
    }
  };

  if (isLoading) {
    return <PageLoader message="Loading list..." />;
  }

  if (error || !data?.data) {
    return (
      <ErrorMessage
        message="Failed to load list. Please try again."
        retry={() => refetch()}
      />
    );
  }

  const list = data.data;
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
          <Button variant="danger" onClick={() => setShowDeleteModal(true)}>
            Delete
          </Button>
        </div>
      </div>

      {items.length > 0 ? (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {items.map((item) => (
            <div key={item.id} className="relative group">
              <a href={`/items/${item.id}`} className="block">
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
              </a>
              <button
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
          <a href="/items">
            <Button>Browse Items</Button>
          </a>
        </div>
      )}

      <Modal isOpen={showEditModal} onClose={() => setShowEditModal(false)} title="Edit List">
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
          <div className="flex gap-3 pt-4">
            <Button
              variant="secondary"
              onClick={() => setShowEditModal(false)}
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

      <Modal isOpen={showDeleteModal} onClose={() => setShowDeleteModal(false)} title="Delete List">
        <div className="space-y-4">
          <p className="text-neutral-300">
            Are you sure you want to delete "{list.name}"? This action cannot be undone.
          </p>
          <div className="flex gap-3 pt-4">
            <Button
              variant="secondary"
              onClick={() => setShowDeleteModal(false)}
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