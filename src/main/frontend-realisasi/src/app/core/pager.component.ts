import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

/** Navigasi halaman di bawah tabel: info jumlah data, tombol halaman, dan pilihan jumlah per halaman. page dimulai dari 0. */
@Component({selector:'app-pager',standalone:true,imports:[CommonModule,FormsModule],template:`
<div class="pager" *ngIf="total>0">
  <span class="pager-info">Menampilkan <b>{{from}}–{{to}}</b> dari <b>{{total}}</b> data</span>
  <div class="pager-nav">
    <button class="pager-btn" [disabled]="page===0" (click)="go(page-1)" aria-label="Sebelumnya">‹</button>
    <ng-container *ngFor="let p of pages"><span *ngIf="p<0" class="pager-gap">…</span><button *ngIf="p>=0" class="pager-btn" [class.active]="p===page" (click)="go(p)">{{p+1}}</button></ng-container>
    <button class="pager-btn" [disabled]="page>=lastPage" (click)="go(page+1)" aria-label="Berikutnya">›</button>
  </div>
  <label class="pager-size">Per halaman <select [ngModel]="size" (ngModelChange)="sizeChange.emit(+$event)"><option *ngFor="let s of sizes" [ngValue]="s">{{s}}</option></select></label>
</div>`})
export class PagerComponent {
  @Input() page=0;@Input() size=20;@Input() total=0;@Input() sizes:number[]=[10,20,50,100];
  @Output() pageChange=new EventEmitter<number>();@Output() sizeChange=new EventEmitter<number>();
  get lastPage(){return Math.max(0,Math.ceil(this.total/this.size)-1);}
  get from(){return this.total?this.page*this.size+1:0;}
  get to(){return Math.min(this.total,(this.page+1)*this.size);}
  /** Halaman pertama, terakhir, dan dua di sekitar halaman aktif; -1 = jeda "…". */
  get pages(){const last=this.lastPage,out:number[]=[];
    for(let p=0;p<=last;p++){if(p===0||p===last||Math.abs(p-this.page)<=2)out.push(p);else if(out[out.length-1]!==-1)out.push(-1);}
    return out;}
  go(p:number){if(p>=0&&p<=this.lastPage&&p!==this.page)this.pageChange.emit(p);}
}
