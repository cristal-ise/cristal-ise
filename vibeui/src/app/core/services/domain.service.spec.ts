import { TestBed } from '@angular/core/testing';
import { DomainService } from './domain.service';
import { DefaultService, PagedPathData } from '../../api';
import { of } from 'rxjs';

describe('DomainService', () => {
  let service: DomainService;
  let mockDefaultService: any;

  beforeEach(() => {
    mockDefaultService = {
      domainGet: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        DomainService,
        { provide: DefaultService, useValue: mockDefaultService }
      ]
    });

    service = TestBed.inject(DomainService);
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
    it('should transform PathData rows to TreeNode structure', () => {
      const rows = [
        { path: 'domain/folder', name: 'folder', type: 'domain' as any, uuid: '1' },
        { path: 'domain/folder/file', name: 'file', type: 'item' as any, uuid: '2' }
      ];

      const nodes = service.transformToTreeNodeItems(rows);

      expect(nodes.length).toBe(1);
      expect(nodes[0].label).toBe('folder');
      expect(nodes[0].children?.length).toBe(1);
      expect(nodes[0].children?.[0].label).toBe('file');
      expect(nodes[0].children?.[0].data.uuid).toBe('2');
    });

    it('should handle sorting by path', () => {
      const rows = [
        { path: 'domain/b', name: 'b', type: 'domain' as any },
        { path: 'domain/a', name: 'a', type: 'domain' as any }
      ];

      const nodes = service.transformToTreeNodeItems(rows);

      expect(nodes[0].label).toBe('a');
      expect(nodes[1].label).toBe('b');
    });
  });
});
