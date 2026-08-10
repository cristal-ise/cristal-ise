import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoPipe } from '@jsverse/transloco';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ChartModule } from 'primeng/chart';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, CardModule, ButtonModule, TagModule, ChartModule, TranslocoPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Dashboard implements OnInit {
  stats = [
    { title: 'layout.dashboard.total_revenue', value: '$45,231.89', change: '+20.1%', icon: 'pi pi-dollar', trend: 'up', color: 'indigo' },
    { title: 'layout.dashboard.active_users', value: '23.5k', change: '+18.4%', icon: 'pi pi-users', trend: 'up', color: 'purple' },
    { title: 'layout.dashboard.goal_reach', value: '84%', change: '+12.5%', icon: 'pi pi-bullseye', trend: 'up', color: 'pink' },
    { title: 'layout.dashboard.system_status', value: 'Healthy', change: '0.01%', icon: 'pi pi-check-circle', trend: 'up', color: 'emerald' }
  ];

  chartData: any;
  chartOptions: any;

  recentActivity: { user: string; actionKey: string; actionParams?: any; timeKey: string; timeParams?: any; status: 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' }[] = [
    { user: 'Alex Rivera', actionKey: 'layout.dashboard.created_project', timeKey: 'layout.dashboard.mins_ago', timeParams: { count: 2 }, status: 'success' },
    { user: 'Sarah Chen', actionKey: 'layout.dashboard.closed_ticket', actionParams: { id: '#2341' }, timeKey: 'layout.dashboard.hour_ago', timeParams: { count: 1 }, status: 'info' },
    { user: 'James Wilson', actionKey: 'layout.dashboard.updated_billing', timeKey: 'layout.dashboard.hours_ago', timeParams: { count: 3 }, status: 'warn' },
    { user: 'Maria Garcia', actionKey: 'layout.dashboard.joined_team', timeKey: 'layout.dashboard.hours_ago', timeParams: { count: 5 }, status: 'success' }
  ];

  ngOnInit() {
    this.initCharts();
  }

  private initCharts() {
    const documentStyle = getComputedStyle(document.documentElement);
    
    this.chartData = {
      labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul'],
      datasets: [
        {
          label: 'layout.dashboard.revenue',
          data: [65, 59, 80, 81, 56, 55, 40],
          fill: true,
          borderColor: '#6366f1',
          tension: 0.4,
          backgroundColor: 'rgba(99, 102, 241, 0.1)'
        },
        {
          label: 'layout.dashboard.expenses',
          data: [28, 48, 40, 19, 86, 27, 90],
          fill: true,
          borderColor: '#ec4899',
          tension: 0.4,
          backgroundColor: 'rgba(236, 72, 153, 0.1)'
        }
      ]
    };

    this.chartOptions = {
        maintainAspectRatio: false,
        aspectRatio: 0.6,
        plugins: {
            legend: {
                display: false
            }
        },
        scales: {
            x: {
                display: false
            },
            y: {
                display: false
            }
        }
    };
  }
}

