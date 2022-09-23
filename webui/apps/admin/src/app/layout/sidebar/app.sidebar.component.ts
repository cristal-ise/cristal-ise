import { Component, ElementRef } from '@angular/core';
import { LayoutService } from "../service/app.layout.service";

@Component({
    selector: 'cristalise-sidebar',
    templateUrl: './app.sidebar.component.html'
})
export class AppSidebarComponent {
    constructor(public layoutService: LayoutService, public el: ElementRef) { }
}

