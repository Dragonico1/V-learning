import { AuthProvider } from "./context/AuthContext.jsx";
import { PrefsProvider } from "./context/PrefsContext.jsx";
import { NotifProvider } from "./context/NotifContext.jsx";
import { TutorProvider } from "./context/TutorContext.jsx";
import { Narrador } from "./components/layout/Narrador.jsx";
import AppRouter from "./routes/AppRouter.jsx";

export default function App() {
  return (
    <AuthProvider>
      <PrefsProvider>
        <NotifProvider>
          <TutorProvider>
            <Narrador />
            <AppRouter />
          </TutorProvider>
        </NotifProvider>
      </PrefsProvider>
    </AuthProvider>
  );
}
