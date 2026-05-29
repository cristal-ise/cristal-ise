import { Routes } from '@angular/router';
import { Landing } from './pages/landing/landing';
import { Login } from './pages/login/login';
import { Dashboard } from './pages/dashboard/dashboard';
import { BasicItemList } from './pages/basic-item-list/basic-item-list';
import { BasicItem } from './pages/basic-item/basic-item';
import { AdminLayout } from './layout/admin-layout/admin-layout';
import { Settings } from './pages/settings/settings';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
    { path: '', component: Landing },
    { path: 'login', component: Login },
    {
        path: 'admin',
        component: AdminLayout,
        canActivate: [authGuard],
        children: [
            { path: '', component: Dashboard },
            { path: 'items', component: BasicItemList },
            { path: 'items/:uuid', component: BasicItem },
            { path: 'settings', component: Settings }
        ]
    }
];
