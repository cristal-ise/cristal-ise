# Webui Project original development setup
Based on version 14 of nx.dev, Angular and PrimeNG with sakai-ng application template

## Use nx.dev to create the 'webui' angular project together with the 'admin' application

1. `npx create-nx-workspace webui --preset=angular --appName=admin --style=scss --nxCloud=false`
1. `cd webui` - all pathes bellow use relative path from this directory
1. replace webui with `cristalise` prefix in these files:
   - apps/admin/.eslintrc.json
   - apps/admin/project.json
   - apps/admin/src/app/app.component.ts
   - apps/admin/src/app/nx-welcome.component.ts
   - apps/admin/src/index.html
   - nx.json
   - package.json
1. change component selector rule in `apps/admin/.eslintrc.json` to include attribute
   ```json
        "@angular-eslint/component-selector": [
          "error",
          {
            "type": ["attribute", "element"],
            "prefix": "cristalise",
            "style": "kebab-case"
          }
        ]
   ```
1. change all component selectors to be inline with the aslint rules
1. add routing module to admin, because nx/angular does not generate routing module by default:
   - `npx nx generate module app-routing --flat --project=admin`
1. Add AppRoutingModule to imports in `apps/admin/src/app/app.module.ts`
1. edit file `apps/admin/src/app/app.component.html` and replace nx-welcome selector with router-outlet
   ```xml
   <router-outlet></router-outlet>
   ```
1. add NxWelcomeComponent to Routes in `apps/admin/src/app/app-routing.module.ts`
   ```js
    const routes: Routes = [
      { path: '', component: NxWelcomeComponent },
    ];
   ```

## Add primeng dependencies and sakai-ng layout components

1. `npm install primeng --save`
1. `npm install primeflex --save`
1. `npm install primeicons --save`
1. `cd apps/admin/src`
1. `cp -rf ~sakai-ng/src/assets/layout assets/.`
1. `cp -rf ~sakai-ng/src/app/layout app/.`
1. add this line to `apps/admin/src/index.html` to the head section
   ```xml
   <link id="theme-css" rel="stylesheet" type="text/css" href="assets/layout/styles/theme/lara-light-indigo/theme.css">
   ```
1. add these lines to `apps/admin/src/styles.scss`
   ```scss
   // for primeflex grid system
   $gutter: 1rem;
   // the layout of sakai-ng
   @import "assets/layout/styles/layout/layout.scss";
   // PrimeNG dependencies
   @import "~primeng/resources/primeng.min.css";
   @import "~primeflex/primeflex.scss";
   @import "~primeicons/primeicons.css";
   ```
1. add AppLayoutModule to imports in `apps/admin/src/app/app.module.ts`
1. edit `apps/admin/src/app/app-routing.module.ts`
   1. add AppLayoutComponent to imports of @NgModule
   1. edit Routes by adding AppLayoutComponent and NxWelcomeComponent
   ```js
    const routes: Routes = [
      {
        path: '', component: AppLayoutComponent,
        children: [
          { path: '', component: NxWelcomeComponent }
        ]
      },
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
   ```
1. remove PrimeBlokcs advert from `apps/admin/src/app/layout/menu/app.menu.component.html`
1. remove unused menu items from `apps/admin/src/app/layout/menu/app.menu.component.ts`
1. add ngOnInit() to `apps/admin/src/app/app.component.ts`
   ```js
   ngOnInit() {
     this.primengConfig.ripple = true;
     document.documentElement.style.fontSize = '14px';
   }
   ```

## Add Login component from sakai-ng

1. copy ~sakai-ng/src/app/demo/components/auth/login to apps/admin/src/app/login 
   1. correct imports and component selector
   1. replace logo and welcome message with application logo and title
1. add this routing declaration as the **first** element of Routes array in `apps/admin/src/app/app-routing.module.ts`
   ```js
   { path: 'login', loadChildren: () => import('./login/login.module').then(m => m.LoginModule) },
   ```

## Add rudimentary authentication routing support 
This section is to test essential setup of guard works

1. add publishable library called core: `npx nx generate @nrwl/angular:library core --publishable --importPath @cristalise/core`
1. add guard: `npx nx generate @nrwl/angular:guard guards/Authentication --project=core --implements=CanActivate`
1. make AuthenticationGuard visible outside the core library
   1. add AuthenticationGuard to providers in `libs/core/src/lib/core.module.ts`
   1. add this line to `libs/core/src/index.ts`
      ```js
      export * from './lib/guards/authentication.guard';
      ```
1. implement the empty canActivate() in `libs/core/src/lib/guards/authentication.guard.ts`
   ```js
   canActivate(
     route: ActivatedRouteSnapshot,
     state: RouterStateSnapshot): boolean | UrlTree {
        // Always redirect to the login page
       return this.router.parseUrl('/login');
     }
   ```
1. add `canActivate: [AuthenticationGuard]` to routing of AppLayoutComponent in `apps/admin/src/app/app-routing.module.ts`
   ```js
   path: '', component: AppLayoutComponent, canActivate: [AuthenticationGuard],
   ```

## Add rudimentary authentication support 
This section is to test that the essential setup of authentication service works

1. add service: `npx nx generate @nrwl/angular:service services/CookieAuthentication --project=core`
1. make CookieAuthenticationService visible outside the core library
   1. add CookieAuthenticationService to providers in `libs/core/src/lib/core.module.ts`
   1. add this line to `libs/core/src/index.ts`
      ```js
      export * from './lib/services/cookie-authentication.service';
      ```
1. aj



## Integrate with CRISTAL-iSE authentication 

1. toto
