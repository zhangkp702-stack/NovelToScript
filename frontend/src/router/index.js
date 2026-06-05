import { createRouter, createWebHistory } from "vue-router";
import { hasSessionId } from "../api/auth";
import LoginView from "../views/LoginView.vue";
import RegisterView from "../views/RegisterView.vue";
import WorkbenchView from "../views/WorkbenchView.vue";

const routes = [
  { path: "/", redirect: "/workbench" },
  { path: "/login", name: "login", component: LoginView, meta: { guestOnly: true } },
  { path: "/register", name: "register", component: RegisterView, meta: { guestOnly: true } },
  { path: "/workbench", name: "workbench", component: WorkbenchView, meta: { requiresAuth: true } }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  const loggedIn = hasSessionId();
  if (to.meta.requiresAuth && !loggedIn) {
    return "/login";
  }
  if (to.meta.guestOnly && loggedIn) {
    return "/workbench";
  }
  return true;
});

export default router;
