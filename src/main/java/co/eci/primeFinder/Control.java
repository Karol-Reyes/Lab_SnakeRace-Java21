/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.eci.primeFinder;

import java.util.Scanner;
/**
 *
 */
public class Control extends Thread {
    
    private final static int NTHREADS = 3;
    private final static int MAXVALUE = 30000000;
    private final static int TMILISECONDS = 1000;
    private final int NDATA = MAXVALUE / NTHREADS;
    private PrimeFinderThread pft[];
    

    // para generar las pausas
    private boolean paused = false;
    private int pausedCount = 0;
    private int activeThreads = NTHREADS; // hilos que faltan por terminar

    private Control() {
        super();
        this.pft = new  PrimeFinderThread[NTHREADS];
        int i;
        for(i = 0; i < NTHREADS - 1; i++) {
            PrimeFinderThread elem = new PrimeFinderThread(i * NDATA, (i+1)*NDATA, this);
            pft[i] = elem;
        }
        pft[i] = new PrimeFinderThread(i*NDATA, MAXVALUE + 1, this);
    }
    
    public static Control newControl() {
        return new Control();
    }

    /**
     * Cada PrimeFinder lo llama dentro de su loop.
     * Si el sistema está en pausa, el thread se bloquea (wait())
     * Avisa cuando todos los hilos ya esten detenidos
     */
    public synchronized void checkPause() throws InterruptedException {
        if (paused) {
            pausedCount++;
            if(pausedCount == activeThreads) {
                notifyAll(); //avisa a control que espera que todos se detengan
            }
            while(paused) {
                wait();
            }
            pausedCount--;
        }
    }

    public synchronized void threadFinished() {
        activeThreads--;
        if (paused && pausedCount == activeThreads) {
            notifyAll();
        }
    }

    private synchronized void pauseAndAllStop() throws InterruptedException {
        paused = true;
        while(pausedCount < activeThreads) {
            wait(); //espera a que todos los hilos se detengan
        }
    }

    private synchronized void resumeAll() {
        paused = false;
        notifyAll();
    }

    private boolean anyAlive() {
        for(PrimeFinderThread t : pft) {
            if(t.isAlive()) {
                return true;
            }
        }
        return false;
    }

    private int countPrimes() {
        int total = 0;
        for(PrimeFinderThread t : pft) {
            total += t.getPrimes().size();
        }
        return total;
    }

    @Override
    public void run() {
        for(int i = 0; i < NTHREADS; i++ ) {
            pft[i].start();
        }

        Scanner sc = new Scanner(System.in);
        try {
            while(anyAlive()){
                Thread.sleep(TMILISECONDS);

                if(!anyAlive()) {
                    break;
                }

                pauseAndAllStop();

                System.out.println("--PAUSE-- primos encontrados hasta ahora: " + countPrimes());
                System.out.println("Presiona ENTER para continuar.");
                sc.nextLine();

                resumeAll();
            }

            for (PrimeFinderThread t : pft) {
                t.join();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Total primos encontrados: " + countPrimes());
    }
    
}