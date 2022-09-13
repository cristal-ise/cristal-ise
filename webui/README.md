# Webui Project

## Usefull commands

- `npx nx serve --project=admin`
- `npx nx g @nrwl/angular:guard guards/Authentication --project=core --implements=CanActivate`
- `npx nx g @nrwl/angular:service services/CookieAuthentication --project=core`
- generate publishable library: `npx nx g @nrwl/angular:library itemlist --publishable --importPath @cristalise/itemlist`
- add storybook config to a project (answer yes for all questions): `npx nx g @nrwl/angular:storybook-configuration itemlist`
- generate story for a component (shall be generated automatically):`nx generate @nrwl/angular:stories --name=itemlist`
