import { useState, type ReactNode } from "react";
import {
  AppBar,
  Avatar,
  Box,
  Chip,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Toolbar,
  Typography,
  useMediaQuery,
} from "@mui/material";
import { useTheme } from "@mui/material/styles";
import BoltRoundedIcon from "@mui/icons-material/BoltRounded";
import MenuRoundedIcon from "@mui/icons-material/MenuRounded";
import LogoutRoundedIcon from "@mui/icons-material/LogoutRounded";
import { useAuth } from "../auth/AuthContext";

export interface NavItem {
  label: string;
  icon: ReactNode;
  render: () => ReactNode;
}

const DRAWER_WIDTH = 240;

/**
 * Responsive dashboard shell for operator + admin. Navigation lives in a side drawer:
 * permanent on md+ screens, and a temporary drawer opened by a hamburger on mobile.
 * The selected nav item's panel renders in the scrollable content area.
 */
export default function DashboardShell({
  title,
  roleLabel,
  items,
}: {
  title: string;
  roleLabel: string;
  items: NavItem[];
}) {
  const { user, logout } = useAuth();
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up("md"));
  const [mobileOpen, setMobileOpen] = useState(false);
  const [selected, setSelected] = useState(0);
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null);

  const active = items[selected] ?? items[0];

  const nav = (
    <Box sx={{ width: DRAWER_WIDTH, maxWidth: "100%", display: "flex", flexDirection: "column", height: "100%" }}>
      <Box sx={{ p: 2, display: "flex", alignItems: "center", gap: 1 }}>
        <BoltRoundedIcon color="primary" />
        <Typography variant="h6" noWrap>
          {title}
        </Typography>
      </Box>
      <Divider />
      <List sx={{ flex: 1, px: 1 }}>
        {items.map((it, i) => (
          <ListItemButton
            key={it.label}
            selected={i === selected}
            onClick={() => {
              setSelected(i);
              setMobileOpen(false);
            }}
            sx={{ borderRadius: 2, mb: 0.5 }}
          >
            <ListItemIcon sx={{ minWidth: 40 }}>{it.icon}</ListItemIcon>
            <ListItemText primary={it.label} />
          </ListItemButton>
        ))}
      </List>
    </Box>
  );

  return (
    <Box sx={{ height: "100dvh", display: "flex", bgcolor: "background.default" }}>
      {/* Desktop: in-flow sidebar that actually occupies its width in the flex row.
          (A permanent MUI Drawer renders position:fixed, which collapses to 0 in flex
          and lets the main content slide under it — the bug this replaces.) */}
      {isDesktop ? (
        <Box
          component="nav"
          sx={{
            width: DRAWER_WIDTH,
            flexShrink: 0,
            borderRight: "1px solid",
            borderColor: "divider",
            bgcolor: "background.paper",
            height: "100%",
            overflowY: "auto",
          }}
        >
          {nav}
        </Box>
      ) : (
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
        >
          {nav}
        </Drawer>
      )}

      {/* Main column */}
      <Box sx={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0 }}>
        <AppBar
          position="static"
          elevation={0}
          color="primary"
          sx={{ pt: "var(--safe-top)" }}
        >
          <Toolbar sx={{ gap: 1 }}>
            {!isDesktop && (
              <IconButton color="inherit" edge="start" onClick={() => setMobileOpen(true)}>
                <MenuRoundedIcon />
              </IconButton>
            )}
            <Typography variant="h6" noWrap sx={{ flexGrow: 1 }}>
              {active?.label ?? title}
            </Typography>
            <Chip
              label={roleLabel}
              size="small"
              sx={{ bgcolor: "rgba(255,255,255,.2)", color: "#fff", fontWeight: 700 }}
            />
            <IconButton color="inherit" onClick={(e) => setMenuAnchor(e.currentTarget)}>
              <Avatar sx={{ width: 30, height: 30, bgcolor: "rgba(255,255,255,.25)", fontSize: 14 }}>
                {user?.name.charAt(0).toUpperCase()}
              </Avatar>
            </IconButton>
            <Menu
              anchorEl={menuAnchor}
              open={Boolean(menuAnchor)}
              onClose={() => setMenuAnchor(null)}
            >
              <Box sx={{ px: 2, py: 1 }}>
                <Typography variant="subtitle2">{user?.name}</Typography>
                <Typography variant="caption" color="text.secondary">
                  {user?.email}
                </Typography>
              </Box>
              <Divider />
              <MenuItem onClick={logout}>
                <ListItemIcon>
                  <LogoutRoundedIcon fontSize="small" />
                </ListItemIcon>
                Sign out
              </MenuItem>
            </Menu>
          </Toolbar>
        </AppBar>

        <Box
          sx={{
            flex: 1,
            overflowY: "auto",
            overflowX: "hidden",
            p: { xs: 1.5, md: 3 },
            pb: "calc(var(--safe-bottom) + 16px)",
          }}
        >
          {active?.render()}
        </Box>
      </Box>
    </Box>
  );
}
