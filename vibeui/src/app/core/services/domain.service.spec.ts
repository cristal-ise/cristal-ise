import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { DomainService } from './domain.service';
import { DefaultService, PagedPathData, PathData, ItemAliases } from '../../api';
import { of, throwError } from 'rxjs';
import { provideZonelessChangeDetection } from '@angular/core';
import { ApiErrorService } from './api-error.service';

describe('DomainService', () => {
  let service: DomainService;
  let mockDefaultService: any;

  beforeEach(() => {
    vi.useFakeTimers();
    mockDefaultService = {
      domainGet: vi.fn(),
      domainPathGet: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        DomainService,
        { provide: DefaultService, useValue: mockDefaultService },
        { provide: ApiErrorService, useValue: { handleError: vi.fn() } }
      ]
    });

    service = TestBed.inject(DomainService);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call domainGet with search=tree when getTreeData is called and return raw PathData', () => {
    const mockResponse: PagedPathData = {
      start: 0,
      pageSize: 10,
      totalRows: 1,
      rows: [
        { path: 'domain/test', name: 'test', type: 'domain', url: '' }
      ]
    };
    mockDefaultService.domainGet.mockReturnValue(of(mockResponse));

    service.getTreeData().subscribe(data => {
      expect(data.length).toBe(1);
      expect(data[0].path).toBe('domain/test');
    });

    expect(mockDefaultService.domainGet).toHaveBeenCalledWith({ search: 'tree' });
  });

  describe('transformToTreeNodeItems', () => {
    it('should transform PathData rows to TreeNode structure with routerLink and queryParams', () => {
      const rows: PathData[] = [
        { path: 'domain/folder', name: 'folder', type: 'domain' as any, uuid: '1' },
        { path: 'domain/folder/file', name: 'file', type: 'item' as any, uuid: '2' }
      ];

      const nodes = service.transformToTreeNodeItems(rows);

      expect(nodes.length).toBe(1);
      expect(nodes[0].label).toBe('folder');
      expect((nodes[0] as any).routerLink).toBe('/admin/items');
      expect((nodes[0] as any).queryParams).toEqual({ domainPath: 'domain/folder' });
      expect(nodes[0].children?.length).toBe(1);
      expect(nodes[0].children?.[0].label).toBe('file');
      expect((nodes[0].children?.[0] as any).routerLink).toBe('/admin/items');
      expect((nodes[0].children?.[0] as any).queryParams).toEqual({ domainPath: 'domain/folder/file' });
      expect(nodes[0].children?.[0].data.uuid).toBe('2');
    });

    it('should handle sorting by path', () => {
      const rows: PathData[] = [
        { path: 'domain/b', name: 'b', type: 'domain' as any },
        { path: 'domain/a', name: 'a', type: 'domain' as any },
      ];

      const nodes = service.transformToTreeNodeItems(rows);

      expect(nodes[0].label).toBe('a');
      expect(nodes[1].label).toBe('b');
    });
  });

  describe('resolveAliases', () => {
    it('should call domainPathGet with correct parameters', () => {
      const uuids = ['uuid1', 'uuid2'];
      const mockAliases: ItemAliases[] = [
        { uuid: 'uuid1', name: 'Name 1' },
        { uuid: 'uuid2', name: 'Name 2' }
      ];
      mockDefaultService.domainPathGet.mockReturnValue(of(mockAliases));

      service.resolveAliases(uuids).subscribe(res => {
        expect(res).toEqual(mockAliases);
      });

      expect(mockDefaultService.domainPathGet).toHaveBeenCalledWith({
        path: 'aliases',
        search: "['uuid1','uuid2']"
      });
    });
  });

  describe('resolveUuid', () => {
    it('should batch multiple calls within 100ms', () => {
      const mockAliases: ItemAliases[] = [
        { uuid: 'uuid1', name: 'Name 1' },
        { uuid: 'uuid2', name: 'Name 2' }
      ];
      mockDefaultService.domainPathGet.mockReturnValue(of(mockAliases));

      let res1, res2;
      service.resolveUuid('uuid1').subscribe(val => res1 = val);
      service.resolveUuid('uuid2').subscribe(val => res2 = val);

      expect(mockDefaultService.domainPathGet).not.toHaveBeenCalled();

      vi.advanceTimersByTime(100);

      expect(mockDefaultService.domainPathGet).toHaveBeenCalledTimes(1);
      expect(res1).toBe('Name 1');
      expect(res2).toBe('Name 2');
    });

    it('should cache resolved UUIDs', () => {
      const mockAliases: ItemAliases[] = [
        { uuid: 'uuid1', name: 'Name 1' }
      ];
      mockDefaultService.domainPathGet.mockReturnValue(of(mockAliases));

      let res1;
      service.resolveUuid('uuid1').subscribe(val => res1 = val);
      vi.advanceTimersByTime(100);

      expect(mockDefaultService.domainPathGet).toHaveBeenCalledTimes(1);
      expect(res1).toBe('Name 1');

      // Second call for same UUID
      let res2;
      service.resolveUuid('uuid1').subscribe(val => res2 = val);
      vi.advanceTimersByTime(100);

      expect(mockDefaultService.domainPathGet).toHaveBeenCalledTimes(1); // Still 1
      expect(res2).toBe('Name 1');
    });

    it('should fall back to UUID if resolution fails', () => {
      mockDefaultService.domainPathGet.mockReturnValue(of([]));

      let res;
      service.resolveUuid('unknown-uuid').subscribe(val => res = val);
      vi.advanceTimersByTime(100);

      expect(res).toBe('unknown-uuid');
    });
  });
});
