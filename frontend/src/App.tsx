import { Box, CircularProgress } from "@mui/material";
import { useAuth } from "./auth/AuthContext";
import ConsumerApp from "./views/ConsumerApp";
import OperatorApp from "./views/OperatorApp";
import AdminApp from "./views/AdminApp";

/**
 * Role-aware router. Anonymous visitors and USER accounts get the consumer map app;
 * OPERATOR and ADMIN accounts land on their respective dashboards.
 */
export default function App() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <Box
        sx={{
          height: "100dvh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  if (user?.role === "ADMIN" || user?.role === "SUPER_ADMIN") return <AdminApp />;
  if (user?.role === "OPERATOR") return <OperatorApp />;
  return <ConsumerApp />;
}
