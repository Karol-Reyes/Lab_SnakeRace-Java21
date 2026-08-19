package co.eci.primeFinder;

import java.util.LinkedList;
import java.util.List;

public class PrimeFinderThread extends Thread{
	
	int a,b;
	private List<Integer> primes;
	private final Control control;

	public PrimeFinderThread(int a, int b, Control control) {
		super();
        this.primes = new LinkedList<>();
		this.a = a;
		this.b = b;
		this.control = control;
	}
    
    @Override
	public void run(){
        try{
            for (int i= a; i < b ;i++){						
                if (isPrime(i)){
                    primes.add(i);
                }
                control.checkPause(); //para bloquear (con wait())
            }
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        } finally {
            control.threadFinished();
        }
	}
	
	boolean isPrime(int n) {
	    boolean ans;
            if (n > 2) { 
                ans = n%2 != 0;
                for(int i = 3; ans && i*i <= n; i+=2 ) {
                    ans = n % i != 0;
                }
            } else {
                ans = n == 2;
            }
	    return ans;
	}

	public List<Integer> getPrimes() {
		return primes;
	}
	
}