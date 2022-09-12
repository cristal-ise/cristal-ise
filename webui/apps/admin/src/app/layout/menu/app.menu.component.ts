import { OnInit } from '@angular/core';
import { Component } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { LayoutService } from '../service/app.layout.service';
import { LookupService, LookupData, Logger } from '@cristalise/core';

@Component({
  selector: 'cristalise-menu',
  templateUrl: './app.menu.component.html'
})
export class AppMenuComponent implements OnInit {

  public model: MenuItem[] = [];

  /**
   * Variable used during buildMenu() to store current index, i.e. the actual processed item,
   * between recursive calls.
   */
  private currentIdx = -1
  private level = 0 // only for debugging
  private log = new Logger('@cristalise/admin', 'AppMenuComponent')

  constructor(
    private lookup: LookupService,
    public layoutService: LayoutService
  ) {}

  ngOnInit() {
    this.fetchDomainTree()
  }

  fetchDomainTree() {
    this.lookup.getDomainTree('/').subscribe({
      next: (data) => {
        if (data.rows.length != 0) {
          this.log.debug('fetchDomainTree()', 'data.rows.length:'+data.rows.length)
          this.model = this.buildMenu(data.rows, this.currentIdx)
        }
      },
      error: (error) => {
        this.log.error('fetchDomainTree()', error);
        //TODO: show error message to user
      },
    });
  }

  private buildMenu(rows: LookupData[], parentIdx: number): MenuItem[] {
    const items: MenuItem[] = [];
    this.level++

    do {
      this.currentIdx++
      const levelIdx = this.currentIdx
      const currentData = rows[this.currentIdx]
      this.log.debug('buildMenu()', '    idx:'+this.currentIdx, '#'+this.level+'(idx:'+levelIdx+')', 'parentIdx:'+parentIdx, 'current:'+currentData.path)

      const newMenuItem = this.getMenuItem(rows[this.currentIdx]);

      if (this.shallGoDeeper(rows)) {
        newMenuItem.items = this.buildMenu(rows, levelIdx)
      }

      if (newMenuItem.items)  newMenuItem.icon = 'pi pi-fw pi-folder'

      items.push(newMenuItem)
    }
    while (this.stayThisLevel(rows, parentIdx))

    //this.log.debug('buildMenu()', '    idx:'+this.currentIdx, '#'+this.level+'(idx:'+levelIdx+')', 'parentIdx:'+parentIdx)
    this.level--

    return items
  }

  private shallGoDeeper(rows: LookupData[]): boolean {
    if (this.currentIdx + 1 == rows.length) return false;

    const currentPath = rows[this.currentIdx].path
    const nextPath = rows[this.currentIdx+1].path
    const returnVal = nextPath.startsWith(currentPath)

    this.log.debug('shallGoDeeper()', returnVal, 'current:'+currentPath, 'next:'+nextPath)

    return returnVal
  }

  private stayThisLevel(rows: LookupData[], parentIdx: number): boolean {
    if (this.currentIdx + 1 == rows.length) return false;
    if (parentIdx == -1) return false

    const parentPath = rows[parentIdx].path
    const nextPath = rows[this.currentIdx+1].path
    const returnVal = nextPath.startsWith(parentPath)

    this.log.debug('stayThisLevel()', returnVal, '#'+this.level, 'parentIdx:'+parentIdx, 'parentPath:'+parentPath, 'nextPath:'+nextPath)

    return returnVal
  }

  private getMenuItem(lookupData: LookupData): MenuItem {
    return {
      'label': lookupData.name,
      // 'routerLink': lookupData.path,
      'icon': 'pi pi-fw pi-list',
      // 'command': (ev?: any) => this.fetchSubmenu(ev)
    };
  }
}
