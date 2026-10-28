package br.com.managerfoot.bancopadrao;

import java.util.List;

/**
 * Listas de nomes e sobrenomes comuns usadas para montar os nomes fictícios dos jogadores (SPEC
 * §4.1, rev. 30). Não é lista de jogadores reais; uma combinação pode coincidir com alguém real.
 */
enum Idioma {

	PORTUGUES(List.of("Adriano", "Alex", "André", "Bruno", "Caio", "Carlos", "César", "Cláudio", "Daniel", "Diego",
			"Douglas", "Edson", "Eduardo", "Fábio", "Felipe", "Fernando", "Gabriel", "Gilberto", "Gustavo", "Hélio",
			"Hugo", "Igor", "Jair", "João", "Jorge", "José", "Júlio", "Leandro", "Leonardo", "Lucas", "Luís", "Marcelo",
			"Marcos", "Mateus", "Maurício", "Nélson", "Otávio", "Paulo", "Pedro", "Rafael", "Renato", "Ricardo",
			"Roberto", "Rodrigo", "Rogério", "Sandro", "Sérgio", "Tiago", "Valter", "Vítor", "Wagner", "Wílson",
			"Anderson", "Cléber", "Danilo", "Everton", "Fabrício", "Juninho", "Márcio", "Nuno"),
			List.of("Almeida", "Andrade", "Araújo", "Barbosa", "Barros", "Batista", "Bezerra", "Brandão", "Cabral",
					"Campos", "Cardoso", "Carvalho", "Castanheira", "Castro", "Correia", "Costa", "Coutinho", "Cunha",
					"Damasceno", "Dias", "Duarte", "Fernandes", "Ferraz", "Ferreira", "Figueiredo", "Fonseca", "Freitas",
					"Furtado", "Gomes", "Gonçalves", "Guimarães", "Gusmão", "Lacerda", "Leite", "Lima", "Lobato",
					"Lopes", "Macedo", "Machado", "Magalhães", "Martins", "Matos", "Medeiros", "Mendes", "Mesquita",
					"Monteiro", "Moraes", "Moreira", "Nascimento", "Neves", "Nogueira", "Oliveira", "Pacheco", "Pereira",
					"Pimentel", "Pinheiro", "Pinto", "Prates", "Queiroz", "Quintela", "Ramos", "Rangel", "Ribeiro",
					"Rocha", "Sampaio", "Santana", "Santos", "Siqueira", "Soares", "Tavares", "Teixeira", "Valadares",
					"Vasconcelos", "Viana", "Vieira")),

	ESPANHOL(List.of("Agustín", "Alberto", "Alejandro", "Álvaro", "Andrés", "Ángel", "Antonio", "Ariel", "Bruno",
			"Carlos", "Cristian", "Damián", "Daniel", "David", "Diego", "Emiliano", "Enrique", "Esteban", "Facundo",
			"Federico", "Fernando", "Franco", "Gabriel", "Gonzalo", "Guillermo", "Hernán", "Hugo", "Ignacio", "Iván",
			"Javier", "Jesús", "Joaquín", "Jorge", "José", "Juan", "Julián", "Leandro", "Lorenzo", "Lucas", "Luis",
			"Manuel", "Marcelo", "Mario", "Martín", "Matías", "Mauro", "Miguel", "Nicolás", "Pablo", "Pedro", "Rafael",
			"Ramiro", "Raúl", "Ricardo", "Rodrigo", "Santiago", "Sergio", "Tomás", "Valentín", "Víctor"),
			List.of("Acosta", "Aguirre", "Álvarez", "Arias", "Benítez", "Blanco", "Bravo", "Cabrera", "Campos",
					"Castillo", "Castro", "Cortés", "Delgado", "Díaz", "Domínguez", "Escobar", "Espinoza", "Fernández",
					"Flores", "Franco", "Fuentes", "Gallardo", "García", "Giménez", "Gómez", "González", "Gutiérrez",
					"Herrera", "Ibáñez", "Iglesias", "Jiménez", "Juárez", "Ledesma", "López", "Luna", "Maldonado",
					"Márquez", "Martínez", "Medina", "Méndez", "Molina", "Montero", "Morales", "Moreno", "Muñoz",
					"Navarro", "Núñez", "Ortega", "Ortiz", "Pérez", "Ponce", "Quiroga", "Ramírez", "Ramos", "Ríos",
					"Rivas", "Rodríguez", "Romero", "Rojas", "Ruiz", "Salas", "Sánchez", "Sosa", "Suárez", "Torres",
					"Valdez", "Vargas", "Vega", "Villalba", "Zapata")),

	INGLES(List.of("Aaron", "Adam", "Alan", "Alex", "Andrew", "Ben", "Callum", "Charlie", "Chris", "Connor", "Daniel",
			"David", "Dean", "Ellis", "Ethan", "Gary", "George", "Harry", "Harvey", "Jack", "Jacob", "Jake", "James",
			"Jamie", "Jason", "Joe", "John", "Jordan", "Josh", "Kevin", "Kieran", "Lee", "Leon", "Lewis", "Liam", "Luke",
			"Mark", "Mason", "Matt", "Michael", "Nathan", "Neil", "Oliver", "Owen", "Paul", "Peter", "Reece", "Rhys",
			"Ricky", "Robert", "Ryan", "Sam", "Scott", "Sean", "Steve", "Ted", "Tom", "Tyler", "Wayne", "Will"),
			List.of("Adams", "Allen", "Armstrong", "Atkinson", "Bailey", "Baker", "Barker", "Barnes", "Bennett", "Brooks",
					"Butler", "Carter", "Chapman", "Clarke", "Cole", "Collins", "Cook", "Cooper", "Davies", "Dawson",
					"Dixon", "Edwards", "Ellis", "Evans", "Fisher", "Fletcher", "Foster", "Fox", "Graham", "Grant",
					"Gray", "Green", "Hall", "Harris", "Harrison", "Hill", "Holmes", "Hughes", "Hunt", "Jackson",
					"James", "Johnson", "Jones", "Kelly", "King", "Knight", "Lewis", "Marshall", "Mason", "Mills",
					"Mitchell", "Moore", "Morgan", "Morris", "Murphy", "Owen", "Palmer", "Parker", "Phillips", "Price",
					"Reid", "Richards", "Roberts", "Robinson", "Russell", "Shaw", "Simpson", "Smith", "Stevens",
					"Taylor", "Thompson", "Turner", "Walker", "Ward", "Webb", "White", "Wilson", "Wood", "Wright",
					"Young")),

	ITALIANO(List.of("Alberto", "Alessandro", "Alessio", "Andrea", "Angelo", "Antonio", "Bruno", "Carlo", "Claudio",
			"Cristian", "Daniele", "Davide", "Domenico", "Emanuele", "Enrico", "Fabio", "Fabrizio", "Federico",
			"Filippo", "Francesco", "Gabriele", "Giacomo", "Gianluca", "Giorgio", "Giovanni", "Giuseppe", "Leonardo",
			"Lorenzo", "Luca", "Luigi", "Manuel", "Marco", "Mario", "Massimo", "Matteo", "Mattia", "Maurizio",
			"Michele", "Nicola", "Paolo", "Pietro", "Raffaele", "Riccardo", "Roberto", "Salvatore", "Samuele",
			"Simone", "Stefano", "Tommaso", "Valerio", "Vincenzo", "Walter"),
			List.of("Barbieri", "Bassi", "Bellini", "Benedetti", "Bernardi", "Bianchi", "Bruno", "Caputo", "Carbone",
					"Caruso", "Cattaneo", "Colombo", "Conte", "Conti", "Costa", "D'Angelo", "De Luca", "De Rosa",
					"Esposito", "Fabbri", "Farina", "Ferrara", "Ferrari", "Ferri", "Fontana", "Galli", "Gallo",
					"Gentile", "Giordano", "Grasso", "Greco", "Leone", "Lombardi", "Longo", "Mancini", "Marchetti",
					"Mariani", "Marino", "Martini", "Messina", "Monti", "Moretti", "Morelli", "Neri", "Palumbo",
					"Pellegrini", "Rinaldi", "Rizzi", "Rizzo", "Romano", "Rossi", "Russo", "Sala", "Santoro", "Serra",
					"Silvestri", "Testa", "Vitale"));

	private final List<String> nomes;
	private final List<String> sobrenomes;

	Idioma(List<String> nomes, List<String> sobrenomes) {
		this.nomes = nomes;
		this.sobrenomes = sobrenomes;
	}

	List<String> nomes() {
		return nomes;
	}

	List<String> sobrenomes() {
		return sobrenomes;
	}
}
