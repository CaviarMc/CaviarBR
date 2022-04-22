package fr.caviar.br.utils.observable;

public interface Observable {

	void observe(String name, Observer observer);

	Observer unobserve(String name);

	@FunctionalInterface
	public interface Observer {
		void changed() throws Exception;
	}

}