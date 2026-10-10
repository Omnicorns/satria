import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guard';
export const routes: Routes = [
  {path:'', pathMatch:'full', redirectTo:'dashboard'},
  {path:'login', canActivate:[guestGuard], data:{bare:true}, loadComponent:()=>import('./pages/login/login.page').then(m=>m.LoginPage)},
  {path:'dashboard', canActivate:[authGuard], data:{title:'Executive Dashboard', section:'Group Overview'}, loadComponent:()=>import('./pages/dashboard/dashboard.page').then(m=>m.DashboardPage)},
  {path:'rka', canActivate:[authGuard], data:{title:'Monitoring RKA', section:'Budget & RKA'}, loadComponent:()=>import('./pages/rka/rka.page').then(m=>m.RkaPage)},
  {path:'pekerjaan', canActivate:[authGuard], data:{title:'CAPEX / OPEX', section:'Budget & RKA'}, loadComponent:()=>import('./pages/pekerjaan/pekerjaan.page').then(m=>m.PekerjaanPage)},
  {path:'master', canActivate:[authGuard], data:{title:'Master Data', section:'Pengaturan'}, loadComponent:()=>import('./pages/master/master.page').then(m=>m.MasterPage)},
  {path:'pengguna', canActivate:[authGuard], data:{title:'Pengguna & Departemen AD', section:'Pengaturan'}, loadComponent:()=>import('./pages/pengguna/pengguna.page').then(m=>m.PenggunaPage)},
  {path:'**', redirectTo:'dashboard'}
];
