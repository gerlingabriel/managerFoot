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
					"Silvestri", "Testa", "Vitale")),

	// rev. 73: idiomas dos países gerados só para a competição continental (§8.3.6)
	ALEMAO(List.of("Alexander", "Andreas", "Benjamin", "Bernd", "Christian", "Daniel", "David", "Dennis", "Dirk",
			"Dominik", "Fabian", "Felix", "Florian", "Frank", "Jan", "Jens", "Jonas", "Julian", "Kai", "Klaus", "Lars",
			"Leon", "Lukas", "Manuel", "Marco", "Mario", "Markus", "Martin", "Mathias", "Max", "Michael", "Niklas",
			"Oliver", "Patrick", "Paul", "Philipp", "Ralf", "Sebastian", "Stefan", "Sven", "Thomas", "Tim", "Tobias",
			"Uwe", "Lennart", "Moritz", "Timo", "Kevin"),
			List.of("Bauer", "Becker", "Braun", "Fischer", "Frank", "Friedrich", "Fuchs", "Günther", "Hahn", "Hartmann",
					"Hoffmann", "Huber", "Jung", "Kaiser", "Keller", "Klein", "Koch", "Köhler", "König", "Krause",
					"Krüger", "Lange", "Lehmann", "Maier", "Meyer", "Möller", "Müller", "Neumann", "Peters", "Richter",
					"Roth", "Schäfer", "Schmidt", "Schmitt", "Schneider", "Scholz", "Schröder", "Schulz", "Schumacher",
					"Schwarz", "Vogel", "Wagner", "Walter", "Weber", "Weiß", "Werner", "Wolf", "Zimmermann")),

	FRANCES(List.of("Adrien", "Alexandre", "Antoine", "Arnaud", "Baptiste", "Benoît", "Bruno", "Cédric", "Christophe",
			"Clément", "Damien", "David", "Didier", "Dylan", "Emmanuel", "Éric", "Fabien", "Florian", "François",
			"Frédéric", "Guillaume", "Hugo", "Jérémy", "Jérôme", "Julien", "Kévin", "Laurent", "Loïc", "Lucas",
			"Mathieu", "Maxime", "Mickaël", "Nicolas", "Olivier", "Pascal", "Patrice", "Pierre", "Quentin", "Raphaël",
			"Romain", "Sébastien", "Stéphane", "Sylvain", "Théo", "Thierry", "Thomas", "Valentin", "Yann"),
			List.of("André", "Bernard", "Bertrand", "Blanc", "Bonnet", "Boyer", "Chevalier", "Clément", "Dubois",
					"Dumont", "Dupont", "Durand", "Fabre", "Faure", "Fontaine", "Fournier", "François", "Garnier",
					"Gauthier", "Girard", "Guérin", "Henry", "Lambert", "Laurent", "Lefebvre", "Legrand", "Leroy",
					"Martin", "Masson", "Mathieu", "Mercier", "Michel", "Morel", "Moreau", "Muller", "Perrin", "Petit",
					"Richard", "Robert", "Rousseau", "Roux", "Simon", "Thomas", "Vincent")),

	NEERLANDES(List.of("Arjen", "Bart", "Bas", "Bram", "Daan", "Dirk", "Erik", "Frank", "Frenkie", "Gijs", "Hans",
			"Jasper", "Jeroen", "Joost", "Jordi", "Jos", "Kees", "Koen", "Lars", "Luuk", "Maarten", "Marco", "Mark",
			"Martijn", "Matthijs", "Niels", "Pieter", "Ruben", "Ruud", "Sander", "Sem", "Stijn", "Teun", "Thijs", "Tim",
			"Tom", "Vincent", "Wim", "Wout", "Yannick"),
			List.of("Bakker", "Bos", "Brouwer", "de Boer", "de Bruijn", "de Graaf", "de Groot", "de Jong", "de Vries",
					"de Wit", "Dekker", "Dijkstra", "Hendriks", "Jacobs", "Jansen", "Janssens", "Kok", "Koster",
					"Maes", "Meijer", "Mertens", "Mulder", "Peeters", "Peters", "Smit", "Smits", "Teunissen", "van Dam",
					"van den Berg", "van der Linden", "van Dijk", "van Leeuwen", "Verhoeven", "Vermeulen", "Visser",
					"Willems", "Wouters")),

	TURCO(List.of("Ahmet", "Ali", "Arda", "Barış", "Burak", "Can", "Cem", "Emre", "Enes", "Ercan", "Erkan", "Fatih",
			"Furkan", "Gökhan", "Hakan", "Halil", "Hasan", "İbrahim", "İsmail", "Kaan", "Kerem", "Mehmet", "Mert",
			"Murat", "Mustafa", "Okan", "Onur", "Orhan", "Ozan", "Selçuk", "Semih", "Serkan", "Sinan", "Tolga", "Tuncay",
			"Umut", "Volkan", "Yusuf"),
			List.of("Acar", "Akın", "Aksoy", "Arslan", "Aslan", "Aydın", "Bulut", "Çelik", "Demir", "Doğan", "Erdem",
					"Güler", "Güneş", "Kaplan", "Kara", "Karaca", "Kaya", "Keskin", "Koç", "Korkmaz", "Kurt", "Özdemir",
					"Özkan", "Öztürk", "Polat", "Şahin", "Tekin", "Topal", "Turan", "Uçar", "Yalçın", "Yavuz",
					"Yıldırım", "Yıldız", "Yılmaz", "Yüksel")),

	GREGO(List.of("Alexandros", "Anastasios", "Andreas", "Angelos", "Antonis", "Christos", "Dimitris", "Georgios",
			"Giannis", "Ilias", "Kostas", "Lazaros", "Manolis", "Michalis", "Nikos", "Panagiotis", "Pantelis", "Pavlos",
			"Petros", "Sotiris", "Stavros", "Stelios", "Thanasis", "Theodoros", "Vangelis", "Vasilis", "Yiorgos",
			"Zisis"),
			List.of("Alexiou", "Anagnostou", "Antoniou", "Christodoulou", "Dimitriou", "Georgiou", "Ioannou",
					"Karagiannis", "Konstantinou", "Makris", "Michailidis", "Nikolaidis", "Oikonomou", "Pappas",
					"Papadakis", "Papadopoulos", "Papageorgiou", "Papanikolaou", "Pavlidis", "Petridis", "Samaras",
					"Stavrou", "Theodorou", "Vasileiou", "Vlachos", "Zervas")),

	TCHECO(List.of("Adam", "Aleš", "David", "Filip", "František", "Jakub", "Jan", "Jaroslav", "Jiří", "Josef",
			"Karel", "Lukáš", "Marek", "Martin", "Matěj", "Michal", "Milan", "Miroslav", "Ondřej", "Pavel", "Petr",
			"Radek", "Roman", "Tomáš", "Václav", "Vladimír", "Vojtěch", "Zdeněk"),
			List.of("Bartoš", "Beneš", "Černý", "Doležal", "Dvořák", "Fiala", "Horák", "Hájek", "Jelínek", "Kolář",
					"Král", "Krejčí", "Kučera", "Marek", "Navrátil", "Novák", "Novotný", "Pokorný", "Pospíšil", "Procházka",
					"Růžička", "Sedláček", "Svoboda", "Veselý", "Zeman")),

	POLONES(List.of("Adam", "Bartosz", "Damian", "Dawid", "Grzegorz", "Hubert", "Jakub", "Jan", "Jarosław", "Kacper",
			"Kamil", "Krzysztof", "Łukasz", "Maciej", "Marcin", "Marek", "Mateusz", "Michał", "Paweł", "Piotr",
			"Przemysław", "Rafał", "Robert", "Sebastian", "Szymon", "Tomasz", "Wojciech", "Zbigniew"),
			List.of("Dąbrowski", "Grabowski", "Jankowski", "Jabłoński", "Kaczmarek", "Kamiński", "Kowalczyk",
					"Kowalski", "Kozłowski", "Krawczyk", "Lewandowski", "Mazur", "Michalski", "Nowak", "Nowicki",
					"Pawłowski", "Piotrowski", "Szymański", "Wieczorek", "Wiśniewski", "Wojciechowski", "Wójcik",
					"Woźniak", "Zając", "Zieliński")),

	NORUEGUES(List.of("Anders", "Bjørn", "Eirik", "Erlend", "Even", "Fredrik", "Håkon", "Henrik", "Jonas", "Jørgen",
			"Kjetil", "Kristian", "Lars", "Magnus", "Marius", "Martin", "Mats", "Morten", "Ole", "Ørjan", "Petter",
			"Rune", "Sander", "Sindre", "Sondre", "Stian", "Thomas", "Tor", "Vegard"),
			List.of("Andersen", "Berg", "Bakke", "Dahl", "Eriksen", "Haugen", "Hagen", "Halvorsen", "Hansen", "Jensen",
					"Johansen", "Johnsen", "Karlsen", "Larsen", "Lie", "Moen", "Nilsen", "Olsen", "Pedersen", "Solberg",
					"Strand", "Sæther", "Thorsen", "Andreassen", "Kristiansen"));

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
