import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import './styles.css'

const SERVICE_WORKER_CLEANUP_KEY = 'admin_sw_cleanup_reloaded'

async function removeLegacyServiceWorker(): Promise<boolean> {
  if (!('serviceWorker' in navigator)) return true

  const controlledByServiceWorker = navigator.serviceWorker.controller !== null
  const registrations = await navigator.serviceWorker.getRegistrations()
  await Promise.all(registrations.map(registration => registration.unregister()))

  if ('caches' in window) {
    const cacheNames = await caches.keys()
    await Promise.all(cacheNames.map(cacheName => caches.delete(cacheName)))
  }

  if (controlledByServiceWorker && !sessionStorage.getItem(SERVICE_WORKER_CLEANUP_KEY)) {
    sessionStorage.setItem(SERVICE_WORKER_CLEANUP_KEY, 'true')
    window.location.reload()
    return false
  }

  sessionStorage.removeItem(SERVICE_WORKER_CLEANUP_KEY)
  return true
}

async function bootstrap() {
  try {
    if (!(await removeLegacyServiceWorker())) return
  } catch (error) {
    console.warn('Không thể dọn Service Worker cũ:', error)
  }

  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <App />
    </StrictMode>
  )
}

bootstrap()
