import { Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';

import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';
import { ButtonModule } from 'primeng/button';

import { ActivePlanViewComponent } from './active-plan-view/active-plan-view.component';
import { PlanListViewComponent } from './plan-list-view/plan-list-view.component';
import { PlanCreationFormComponent } from './plan-creation-form/plan-creation-form.component';

@Component({
  selector: 'app-plans-page',
  standalone: true,
  imports: [
    CommonModule,
    Tabs,
    TabList,
    Tab,
    TabPanels,
    TabPanel,
    ButtonModule,
    ActivePlanViewComponent,
    PlanListViewComponent,
    PlanCreationFormComponent,
  ],
  templateUrl: './plans-page.component.html',
  styleUrl: './plans-page.component.scss',
})
export default class PlansPageComponent {
  @ViewChild(ActivePlanViewComponent) activePlanView!: ActivePlanViewComponent;
  @ViewChild(PlanListViewComponent) planListView!: PlanListViewComponent;

  activeTab: string | number = '0';
  showCreateDialog = false;

  openCreateDialog(): void {
    this.showCreateDialog = true;
  }

  onPlanCreated(): void {
    this.activeTab = '1';
    this.planListView?.loadPlans();
    this.activePlanView?.loadActivePlan();
  }

  onTabChange(tabValue: string | number | undefined): void {
    if (tabValue === undefined) return;
    this.activeTab = tabValue;
    if (tabValue === '0') {
      this.activePlanView?.loadActivePlan();
    } else if (tabValue === '1') {
      this.planListView?.loadPlans();
    }
  }
}
