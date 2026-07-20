import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, OnDestroy, viewChild } from '@angular/core';
import { DotLottie } from '@lottiefiles/dotlottie-web';

@Component({
  selector: 'app-race-in-progress-overlay',
  templateUrl: './race-in-progress-overlay.component.html',
  styleUrl: './race-in-progress-overlay.component.less',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RaceInProgressOverlayComponent implements AfterViewInit, OnDestroy {
  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('animationCanvas');
  private animation: DotLottie | null = null;

  ngAfterViewInit(): void {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    DotLottie.setWasmUrl('/assets/dotlottie/dotlottie-player.wasm');
    this.animation = new DotLottie({
      canvas: this.canvas().nativeElement,
      src: '/lottie/Horse_Run_V1.lottie',
      autoplay: true,
      loop: true,
      speed: reducedMotion ? 0.75 : 1.5,
      layout: { fit: 'contain', align: [0.5, 0.5] },
      renderConfig: {
        autoResize: true,
        devicePixelRatio: Math.min(window.devicePixelRatio, 2),
        freezeOnOffscreen: false,
        quality: 90
      }
    });
    this.animation.addEventListener('load', this.play);
  }

  ngOnDestroy(): void {
    this.animation?.removeEventListener('load', this.play);
    this.animation?.destroy();
    this.animation = null;
  }

  private readonly play = (): void => this.animation?.play();
}
