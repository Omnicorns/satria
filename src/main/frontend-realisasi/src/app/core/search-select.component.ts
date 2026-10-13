import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, Input, ViewChild, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface SearchOption { value:any; label:string; }

/**
 * Pengganti <select> untuk daftar panjang (COA, program kerja): ketik nomor atau nama untuk menyaring.
 * Semua kata yang diketik harus muncul di label (tidak peka huruf besar/kecil). Dipakai dengan [(ngModel)].
 */
@Component({selector:'app-search-select',standalone:true,imports:[CommonModule],
  providers:[{provide:NG_VALUE_ACCESSOR,useExisting:forwardRef(()=>SearchSelectComponent),multi:true}],template:`
<div class="search-select" [class.open]="open">
  <input #box type="text" [value]="open?query:selectedLabel" [placeholder]="selectedLabel||placeholder" [disabled]="disabled"
    (focus)="show()" (click)="show()" (input)="query=box.value;active=0" (keydown)="key($event)" autocomplete="off">
  <div class="ss-list" *ngIf="open">
    <button type="button" *ngFor="let o of filtered;let i=index" class="ss-option" [class.active]="i===active" [class.selected]="o.value===value"
      (mousedown)="$event.preventDefault();pick(o)" (mouseenter)="active=i">{{o.label}}</button>
    <div class="ss-empty" *ngIf="!filtered.length">Tidak ada yang cocok dengan "{{query}}"</div>
  </div>
</div>`})
export class SearchSelectComponent implements ControlValueAccessor {
  @Input() options:SearchOption[]=[];
  @Input() placeholder='Ketik untuk mencari';
  @ViewChild('box') box!:ElementRef<HTMLInputElement>;
  value:any=null;query='';open=false;active=0;disabled=false;
  private onChange=(_:any)=>{};private onTouched=()=>{};

  constructor(private host:ElementRef<HTMLElement>){}

  get selectedLabel(){return this.options.find(o=>o.value===this.value)?.label||'';}
  get filtered(){
    const words=this.query.toLowerCase().split(/\s+/).filter(Boolean);
    return this.options.filter(o=>{const l=o.label.toLowerCase();return words.every(w=>l.includes(w));}).slice(0,100);
  }

  show(){if(this.disabled||this.open)return;this.open=true;this.query='';this.active=Math.max(0,this.filtered.findIndex(o=>o.value===this.value));}
  close(){this.open=false;this.query='';this.onTouched();}
  pick(o:SearchOption){this.value=o.value;this.onChange(o.value);this.close();this.box?.nativeElement.blur();}
  key(e:KeyboardEvent){
    if(!this.open){if(e.key==='ArrowDown'||e.key==='Enter'){this.show();e.preventDefault();}return;}
    const n=this.filtered.length;
    if(e.key==='ArrowDown'){this.active=Math.min(n-1,this.active+1);e.preventDefault();this.scroll();}
    else if(e.key==='ArrowUp'){this.active=Math.max(0,this.active-1);e.preventDefault();this.scroll();}
    else if(e.key==='Enter'){if(n)this.pick(this.filtered[this.active]);e.preventDefault();}
    else if(e.key==='Escape'||e.key==='Tab'){this.close();}
  }
  private scroll(){setTimeout(()=>this.host.nativeElement.querySelector('.ss-option.active')?.scrollIntoView({block:'nearest'}));}

  @HostListener('document:mousedown',['$event'])
  outside(e:MouseEvent){if(this.open&&!this.host.nativeElement.contains(e.target as Node))this.close();}

  writeValue(v:any){this.value=v;}
  registerOnChange(fn:any){this.onChange=fn;}
  registerOnTouched(fn:any){this.onTouched=fn;}
  setDisabledState(d:boolean){this.disabled=d;}
}
