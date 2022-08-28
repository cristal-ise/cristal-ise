# Webui Project original development setup
Based on version 14 of nx.dev, Angular and PrimeNG with sakai-ng application template. 
Each stage shall create in a working application.

## Stage 1 - Use nx.dev to create the 'webui' angular project together with the 'admin' application

1. `npx create-nx-workspace webui --preset=angular --appName=admin --style=scss --nxCloud=false`
1. `cd webui` - all pathes bellow use relative path from this directory
1. replace webui with `cristalise` prefix in these files bellow. (Note that you can replace 'cristalise' with your application specific prefix)
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
1. add AppRoutingModule to imports array in `apps/admin/src/app/app.module.ts`
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

## Stage 2 - Add primeng dependencies and sakai-ng layout components
This section is based on the primeng tutorial video: https://youtu.be/yl2f8KKY204 

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

## Stage 3 - Reorganise component files in apps/admin/src/app/layout 
This step is not detailed here, as it requires copy&pasting components files into their specific directory
and changing the imports to succesfully compile the project. At the end the file structure could look like this:
```
├── api
│   └── menuchangeevent.ts
├── config
│   ├── app.config.component.html
│   ├── app.config.component.ts
│   └── config.module.ts
├── footer
│   ├── app.footer.component.html
│   └── app.footer.component.ts
├── menu
│   ├── app.menu.component.html
│   ├── app.menu.component.ts
│   └── app.menuitem.component.ts
├── service
│   ├── app.layout.service.ts
│   └── app.menu.service.ts
├── sidebar
│   ├── app.sidebar.component.html
│   └── app.sidebar.component.ts
├── topbar
│   ├── app.topbar.component.html
│   └── app.topbar.component.ts
├── app.layout.component.html
├── app.layout.component.ts
└── app.layout.module.ts
```

## Stage 4 - Add Login component from sakai-ng

1. copy ~sakai-ng/src/app/demo/components/auth/login to apps/admin/src/app/login 
   1. correct imports and component selector
   1. replace logo and welcome message with application logo and title
1. add this routing declaration as the **first** element of Routes array in `apps/admin/src/app/app-routing.module.ts`
   ```js
   { path: 'login', loadChildren: () => import('./login/login.module').then(m => m.LoginModule) },
   ```

## Stage 5 - Add rudimentary authentication routing support 
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

## Stage 6 - Add rudimentary authentication support 
This section is to test that the essential setup of authentication service works

1. add service: `npx nx generate @nrwl/angular:service services/CookieAuthentication --project=core`
1. make CookieAuthenticationService visible outside the core library
   1. add CookieAuthenticationService to providers in `libs/core/src/lib/core.module.ts`
   1. add this line to `libs/core/src/index.ts`
      ```js
      export * from './lib/services/cookie-authentication.service';
      ```
1. Implement CookieAuthenticationService in `libs/core/src/lib/services/cookie-authentication.service.ts`
   ```js
   export class CookieAuthenticationService {

     public isLoggedIn = false;
     public redirectUrl: string | null = null;

     login(userName: string, password: string): void {
       if (userName && password) {
         this.loggedInUser.next(userName);
         this.isLoggedIn = true;
       }
     }
   }
   ```
1. implement canActivate() in `libs/core/src/lib/guards/authentication.guard.ts`
   ```js
   canActivate(
     route: ActivatedRouteSnapshot,
     state: RouterStateSnapshot): boolean | UrlTree
   {
     // Store the attempted URL for redirecting
     this.authService.redirectUrl = url;

     if (this.authService.isLoggedIn) {
       return true;
     } else {
       // Redirect to the login page
       return this.router.parseUrl('/login');
     }
   }
   ```
1. implement constructor() and submit() in `apps/admin/src/app/login/login-routing.module.ts`
   ```js
   constructor(private router: Router, private authService: CookieAuthenticationService) {}

   submit() {
     this.authService.login(this.user, this.password);
     this.router.navigateByUrl(this.authService.redirectUrl || "/");
   }
   ```
1. edit `apps/admin/src/app/login/login.component.html`
   1. add `(keyup.enter)="submit()"` to p-password element
   1. add `(click)="submit()"` to button element

## Stage 7 - Integrate with CRISTAL-iSE cookie based authentication

1. add proxy configuration using this tutorial: https://nx.dev/angular-tutorial/06-proxy
1. add HttpClient to `CookieAuthenticationService`
   ```js
  constructor(private http: HttpClient) {
   ```
1. reimplement `CookieAuthenticationService.login()`
   ```js
   private loggedInUser = new Subject<string>();

   login(userName: string, password: string): Observable<boolean> {
     const result = new Subject<boolean>();

     this.callLoginPost(userName, password).subscribe({
       next: (value) => {
         if (value['Login'] && value['Login'].uuid) {
           this.loggedInUser.next(value['Login'].uuid);
           this.isLoggedIn = true;
           result.next(true);
         }
         else {
           result.next(false);
         }
       },
       error: (error) => {
         console.error(error);
         result.next(false);
       },
       complete: () => {
         console.debug('login()', 'complete');
       }
     });
     return result.asObservable();
   }

   callLoginPost(agent: string, pwd: string): Observable<any> {
     const url = this.root + '/login';
     // btoa encrypts username and password to base64 - UTF8 Charset
     const body = JSON.stringify({ username: btoa(agent), password: btoa(pwd) });

     return this.http.post(url, body, { withCredentials: true });
   }
   ```
1. add primeng MesseageService to LoginComponent `apps/admin/src/app/login/login.component.ts`
   1. add MessageModule and MessagesModule to the imports array
   1. add MessageService to the providers array
1. add messages member variable to LoginComponent
   ```js
  public messages: Message[] = [];
   ``` 
1. add p-messages element to `apps/admin/src/app/login/login.component.html`
   ```html
   <p-messages [(value)]="messages" [enableService]="false"></p-messages>
   ```
1. add ngOnDestroy handling to LoginComponent
   ```js
   export class LoginComponent implements OnDestroy {
     private readonly onDestroy$: Subject<void> = new Subject();

     ngOnDestroy(): void {
       this.onDestroy$.next();
       this.onDestroy$.complete();
     }
   }
   ```
1. reimplement `LoginComponent.submit()`
   ```js
   submit() {
     this.messages = []

     this.authService.login(this.user, this.password)
     .pipe(takeUntil(this.onDestroy$))
     .subscribe({
       next: (result) => {
         if (result) {
           this.router.navigateByUrl(this.authService.redirectUrl || "/");
         }
         else {
           const message = { severity:'error', summary:'Info', detail:'Invalid username or password' };
           this.messages = [message];
         }
       },
       error: (error) => {
         const message = {severity:'error', summary:'Info', detail: 'Internal server during login'};
         this.messages = [message]
         console.error('submit()', error);
       }
     })
   }
   ```
