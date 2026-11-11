import { RouterProvider } from 'react-router-dom';
import { router } from './router';

export default function AppRoot() {
  return <RouterProvider router={router} />;
}
