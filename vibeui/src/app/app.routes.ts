import { Routes } from '@angular/router';
import { Landing } from './pages/landing/landing';
import { Login } from './pages/login/login';
import { Dashboard } from './pages/dashboard/dashboard';
import { AdminLayout } from './layout/admin-layout/admin-layout';
import { Settings } from './pages/settings/settings';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
    { path: '', component: Landing },
    { path: 'login', component: Login },
    {
        path: 'dashboard',
        component: AdminLayout,
        canActivate: [authGuard],
        children: [
            { path: '', component: Dashboard },
            { path: 'users', component: Dashboard },
            { path: 'settings', component: Settings }
        ]
    }
];
