import { Component } from '@angular/core';

@Component({
  selector: 'cristalise-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  title = 'admin';

  ngOnInit() {
    //this.primengConfig.ripple = true;
    document.documentElement.style.fontSize = '14px';
  }
}
