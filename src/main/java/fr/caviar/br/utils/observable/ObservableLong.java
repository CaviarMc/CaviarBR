package fr.caviar.br.utils.observable;

public class ObservableLong extends AbstractObservable implements Comparable<ObservableLong> {

	private long value;

	public ObservableLong(long value) {
		this.value = value;
	}

	public void set(long value) {
		this.value = value;
		update();
	}

	public void increment() {
		value++;
		update();
	}

	public void decrement() {
		value--;
		update();
	}

	public void add(int value) {
		this.value += value;
		update();
	}

	public void substract(int value) {
		this.value -= value;
		update();
	}

	public long get() {
		return value;
	}

	public double getAsDouble() {
		return (double) value;
	}

	@Override
	public int compareTo(ObservableLong o) {
		return compareTo(o.value);
	}

	public int compareTo(long o) {
		return Long.compare(value, o);
	}

}
