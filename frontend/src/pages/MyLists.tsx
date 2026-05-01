import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useLists, useCreateList, useDeleteList } from '../hooks/useLists';
import { PageLoader } from '../components/common/Loader';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { Button } from '../components/common/Button';
import { Card, CardBody } from '../components/common/Card';
import { Modal } from '../components/common/Modal';
import { Input } from '../components/common/Input';
import type { FavoriteList } from '../types/list';

function ListCard({ list, onDelete }: { list: FavoriteList; onDelete: () => void }) {
  return (
    <Card className="group">
      <CardBody>
        <Link to={`/lists/${list.id}`} className="block">
          <div className="aspect-video bg-neutral-900 rounded-sm mb-4 overflow-hidden">
            {list.items.length > 0 && list.items[0].posterUrl ? (
              <img
                src={list.items[0].posterUrl}
                alt={list.name}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center">
                <span className="text-neutral-700 text-4xl">?</span>
              </div>
            )}
          </div>
          <h3 className="text-neutral-100 font-medium text-lg mb-1 group-hover:text-amber-400 transition-colors">
            {list.name}
          </h3>
          {list.description && (
            <p className="text-neutral-500 text-sm line-clamp-2 mb-2">
              {list.description}
            </p>
          )}
          <p className="text-neutral-600 text-sm">
            {list.items.length} {list.items.length === 1 ? 'item' : 'items'}
          </p>
        </Link>
        <div className="mt-4 flex gap-2">
          <Button
            variant="danger"
            onClick={(e) => {
              e.preventDefault();
              onDelete();
            }}
            className="flex-1"
          >
            Delete
          </Button>
        </div>
      </CardBody>
    </Card>
  );
}

export function MyLists() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { data, isLoading, error, refetch } = useLists(user?.id ?? '');
  const createList = useCreateList();
  const deleteList = useDeleteList();

  const [showModal, setShowModal] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  const handleCreateList = async () => {
    if (!user?.id || !name.trim()) return;
    try {
      await createList.mutateAsync({
        userId: user.id as import('crypto').UUID,
        name: name.trim(),
        description: description.trim() || undefined,
      });
      setShowModal(false);
      setName('');
      setDescription('');
    } catch (err) {
      console.error('Failed to create list:', err);
    }
  };

  const handleDeleteList = async (id: number) => {
    if (!confirm('Are you sure you want to delete this list?')) return;
    try {
      await deleteList.mutateAsync(id);
    } catch (err) {
      console.error('Failed to delete list:', err);
    }
  };

  if (isLoading) {
    return <PageLoader message="Loading your lists..." />;
  }

  if (error) {
    return (
      <ErrorMessage
        message="Failed to load your lists. Please try again."
        retry={() => refetch()}
      />
    );
  }

  const lists = data?.content ?? [];

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold text-neutral-100 tracking-tight mb-2">
            My Lists
          </h1>
          <p className="text-neutral-500">Manage your favorite lists</p>
        </div>
        <Button onClick={() => setShowModal(true)}>Create List</Button>
      </div>

      {lists.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {lists.map((list) => (
            <ListCard key={list.id} list={list} onDelete={() => handleDeleteList(list.id)} />
          ))}
        </div>
      ) : (
        <div className="text-center py-16">
          <p className="text-neutral-500 mb-4">You haven't created any lists yet.</p>
          <Button onClick={() => setShowModal(true)}>Create Your First List</Button>
        </div>
      )}

      <Modal isOpen={showModal} onClose={() => setShowModal(false)} title="Create New List">
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
            <Button variant="secondary" onClick={() => setShowModal(false)} className="flex-1">
              Cancel
            </Button>
            <Button
              onClick={handleCreateList}
              disabled={!name.trim() || createList.isPending}
              isLoading={createList.isPending}
              className="flex-1"
            >
              Create
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}