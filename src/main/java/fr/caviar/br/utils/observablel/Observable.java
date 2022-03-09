package fr.caviar.br.utils.observablel;

public interface Observable {

	void observe(String name, Observer observer);

	void unobserve(String name);

	@FunctionalInterface
	public interface Observer {
		void changed() throws Exception;
	}

}