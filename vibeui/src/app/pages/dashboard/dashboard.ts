import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ChartModule } from 'primeng/chart';

@Component({
  selector: 'app-dashboard',
  imports: [CardModule, ButtonModule, TagModule, ChartModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Dashboard implements OnInit {
  stats = [
    { title: 'Total Revenue', value: '$45,231.89', change: '+20.1%', icon: 'pi pi-dollar', trend: 'up', color: 'indigo' },
    { title: 'Active Users', value: '23.5k', change: '+18.4%', icon: 'pi pi-users', trend: 'up', color: 'purple' },
    { title: 'Goal Reach', value: '84%', change: '+12.5%', icon: 'pi pi-bullseye', trend: 'up', color: 'pink' },
    { title: 'System Status', value: 'Healthy', change: '0.01%', icon: 'pi pi-check-circle', trend: 'up', color: 'emerald' }
  ];

  chartData: any;
  chartOptions: any;

  recentActivity: { user: string; action: string; time: string; status: 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' }[] = [
    { user: 'Alex Rivera', action: 'Created new project', time: '2 mins ago', status: 'success' },
    { user: 'Sarah Chen', action: 'Closed ticket #2341', time: '1 hour ago', status: 'info' },
    { user: 'James Wilson', action: 'Updated billing method', time: '3 hours ago', status: 'warn' },
    { user: 'Maria Garcia', action: 'Joined the team', time: '5 hours ago', status: 'success' }
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
          label: 'Revenue',
          data: [65, 59, 80, 81, 56, 55, 40],
          fill: true,
          borderColor: '#6366f1',
          tension: 0.4,
          backgroundColor: 'rgba(99, 102, 241, 0.1)'
        },
        {
          label: 'Expenses',
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

