import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard:CanActivateFn=(_route,state)=>{
  const auth=inject(AuthService), router=inject(Router);
  return auth.loggedIn ? true : router.createUrlTree(['/login'],{queryParams:{redirect:state.url}});
};

export const guestGuard:CanActivateFn=()=>{
  const auth=inject(AuthService), router=inject(Router);
  return auth.loggedIn ? router.createUrlTree(['/dashboard']) : true;
};
