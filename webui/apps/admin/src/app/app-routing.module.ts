import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { NxWelcomeComponent } from "./nx-welcome.component";
import { AppLayoutComponent } from "./layout/app.layout.component";

import { AuthenticationGuard } from '@cristalise/core';

const routes: Routes = [
  { path: 'login', loadChildren: () => import('./login/login.module').then(m => m.LoginModule) },
  {
    path: '', component: AppLayoutComponent, canActivate: [AuthenticationGuard],
    children: [
      { path: '', component:  NxWelcomeComponent }
    ]
  },
  { path: "**", redirectTo: "/"},
];

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    RouterModule.forRoot(
      routes,
      {
        scrollPositionRestoration: 'enabled',
        anchorScrolling: 'enabled'
      }
    )
  ],
  exports: [RouterModule]
})
export class AppRoutingModule { }
