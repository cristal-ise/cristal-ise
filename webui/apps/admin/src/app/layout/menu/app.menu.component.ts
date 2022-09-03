import { OnInit } from '@angular/core';
import { Component } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { LayoutService } from '../service/app.layout.service';

@Component({
    selector: 'cristalise-menu',
    templateUrl: './app.menu.component.html'
})
export class AppMenuComponent implements OnInit {

    model: MenuItem[] = [];

    constructor(public layoutService: LayoutService) { }

    ngOnInit() {
        this.model = [
            {
                label: 'Home',
                items: [
                    { label: 'Dashboard', icon: 'pi pi-fw pi-home', routerLink: ['/'] }
                ]
            },
            {
                label: 'Descriptions',
                items: []
            },
            {
                label: 'Objects',
                items: []
            }
        ];
    }
}
