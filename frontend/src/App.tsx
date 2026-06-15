// src/App.tsx
import { AuthProvider } from "./context/AuthContext";
import { BalanceProvider } from "./components/BalanceBar/BalanceProvider";
import { GroupNotificationsProvider } from "./context/GroupNotificationsContext";
import AppRouter from "./routes/AppRouter";

function App() {
  return (
    <AuthProvider>
      <BalanceProvider>
        <GroupNotificationsProvider>
          <AppRouter />
        </GroupNotificationsProvider>
      </BalanceProvider>
    </AuthProvider>
  );
}

export default App;
