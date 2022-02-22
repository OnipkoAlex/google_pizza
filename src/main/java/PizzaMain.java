import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class PizzaMain {

    private static Map<String, Integer> likeIngredients = new HashMap<>();
    private static Map<String, Integer> disLikeIngredients = new HashMap<>();
    private static List<Client> satisfied = new ArrayList<>();
    private static List<Client> allClients;

    private static List<Client> fillClientsList(List<String[]> trimString) {

        List<Client> clients = new ArrayList<>();

        for (int i = 1; i < trimString.size(); i += 2) {
            Set<String> allUniqLikeProducts = new HashSet<>();
            Set<String> allUniqDislikeProducts = new HashSet<>();
            Client client = new Client();

            String[] product = trimString.get(i);
            for (int j = 1; j < product.length; j++) {
                allUniqLikeProducts.add(product[j]);
                if (likeIngredients.containsKey(product[j])) {
                    likeIngredients.put(product[j], likeIngredients.get(product[j]) + 1);
                } else {
                    likeIngredients.put(product[j], 1);
                }
            }
            client.setLikes(allUniqLikeProducts);

            String[] product2 = trimString.get(i + 1);
            for (int k = 1; k < product2.length; k++) {
                allUniqDislikeProducts.add(product2[k]);
                if (disLikeIngredients.containsKey(product2[k])) {
                    disLikeIngredients.put(product2[k], disLikeIngredients.get(product2[k]) + 1);
                } else {
                    disLikeIngredients.put(product2[k], 1);
                }
            }
            client.setDislikes(allUniqDislikeProducts);
            clients.add(client);
        }

        likeIngredients =
                likeIngredients.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(
                                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                        LinkedHashMap::new));
        disLikeIngredients =
                disLikeIngredients.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(
                                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                        LinkedHashMap::new));

        return clients;
    }

    private static List<String[]> fileToStringList(Path path) {
        List<String> list;
        if (Files.exists(path)) {
            try {
                list = Files.lines(path).toList();
            } catch (IOException e) {
                return null;
            }
        } else {
            System.out.println("Create and fill file first.");
            return null;
        }

        return list.stream().map(x -> x.split(" ")).toList();
    }

    private static void output(Set<String> ingredients, String out) {

        Path path = Paths.get(out);
        String result = "";

        int count = ingredients.size();
        result = count + " ";

        for (String s : ingredients) {
            result += s + " ";
        }

        result = result.substring(0, result.length() - 1);

        System.out.println(CountSatisfied.countSatisfiedClients(result, allClients) + " satisfied");

        if (Files.exists(path)) {
            try {
                Files.delete(path);
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            Files.write(path, result.getBytes(), StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Client> reEvaluateClients(List<Client> clients) {
        disLikeIngredients = new HashMap<>();

        for (Client client : clients) {
            for (String dislike : client.dislikes) {
                if (disLikeIngredients.containsKey(dislike)) {
                    disLikeIngredients.put(dislike, disLikeIngredients.get(dislike) + 1);
                } else {
                    disLikeIngredients.put(dislike, 1);
                }
            }
        }

        for (Client client : clients) {
            int disLikeScore = 0;
            int notSatisfiedByPeople = 0;
            if (disLikeIngredients.size() == 0) {
                client.setDisLikeScore(disLikeScore);
                continue;
            }
            for (String like : client.getLikes()) {
                for (Map.Entry<String, Integer> entry : disLikeIngredients.entrySet()) {
                    if (entry.getKey().equals(like)) {
                        disLikeScore += entry.getValue();
                    }
                }
            }
            for (String dislike : client.dislikes) {
                for (Client client1 : clients) {
                    for (String like : client1.likes) {
                        if (like.equals(dislike)) {
                            notSatisfiedByPeople++;
                        }
                    }
                }
            }
            client.setDisLikeScore(disLikeScore + notSatisfiedByPeople);
        }

        clients = clients.stream().sorted(Client.Comparators.SCORE).toList();
        //clients.stream().forEach(System.out::println);

        return new ArrayList<>(clients);
    }

    private static List<Client> checkSatisfied(List<Client> clients, Set<String> ingredients) {
        List<Client> toDelete = new ArrayList<>();

        for (Client client : clients) {
            if (ingredients.containsAll(client.getLikes())) {
                satisfied.add(client);
                toDelete.add(client);
            }
        }
        System.out.println("checkSatisfied: " + toDelete.size());
        //toDelete.stream().forEach(System.out::println);
        clients.removeAll(toDelete);
        return clients;
    }

    private static List<Client> cleanClients(List<Client> clients, Set<String> ingredients, Set<String> dislikes) {
        boolean delete = false;
        List<Client> toDelete = new ArrayList<>();

        for (Client client : clients) {
            for (String dislike : client.getDislikes()) {
                for (String ingredient : ingredients) {
                    if (ingredient.equals(dislike)) {
                        delete = true;
                        break;
                    }
                }
                if (delete) break;
            }
            for (String like : client.likes) {
                for (String dislike : dislikes) {
                    if (dislike.equals(like)) {
                        delete = true;
                        break;
                    }
                }
                if (delete) break;
            }
            if (delete) {
                toDelete.add(client);
                delete = false;
            }
        }

        System.out.println("cleanClients: " + toDelete.size());
        //toDelete.stream().forEach(System.out::println);
        clients.removeAll(toDelete);
        return clients;
    }

    private static Map<String, Integer> cleanDislikes(Set<String> ingredients, Map<String, Integer> dislikes) {
        List<String> toDelete = new ArrayList<>();

        for (String ingredient : ingredients) {
            for (Map.Entry<String, Integer> entry : dislikes.entrySet()) {
                if (entry.getKey().equals(ingredient)) toDelete.add(entry.getKey());
            }
        }

        for (String s : toDelete) {
            dislikes.remove(s);
        }

        return new HashMap<>(dislikes);
    }

    private static Set<String> processIngredients(List<Client> clients) {
        Set<String> result = new HashSet<>();
        Map<String, Integer> dislikes = new HashMap<>(disLikeIngredients);
        Set<String> disLikedBySatisfied = new HashSet<>();
        allClients = clients;

        do {
            clients = reEvaluateClients(clients);
            Client client = clients.get(0);
            satisfied.add(client);
            clients.remove(0);
            result.addAll(client.getLikes());
            disLikedBySatisfied.addAll(client.getDislikes());
            //System.out.println("result: " + result);
            clients = cleanClients(clients, result, disLikedBySatisfied);
            clients = checkSatisfied(clients, result);
            System.out.println("Clients satisfied: " + satisfied.size());
            //satisfied.stream().forEach(System.out::println);
            System.out.println();
            dislikes = cleanDislikes(result, dislikes);
        } while (!clients.isEmpty());


        return result;
    }

    public static void main(String[] args) {
        Path path = Paths.get("e_elaborate.in.txt");

        output(processIngredients(fillClientsList(Objects.requireNonNull(fileToStringList(path)))), "src/main/resources/e_output.txt");
    }

    static class Client implements Comparable<Client> {

        private Set<String> likes;
        private Set<String> dislikes;
        private Integer disLikeScore;

        public Set<String> getLikes() {
            return likes;
        }

        public void setLikes(Set<String> likes) {
            this.likes = likes;
        }

        public Set<String> getDislikes() {
            return dislikes;
        }

        public void setDislikes(Set<String> dislikes) {
            this.dislikes = dislikes;
        }

        public Integer getDisLikeScore() {
            return disLikeScore;
        }

        public void setDisLikeScore(Integer disLikeScore) {
            this.disLikeScore = disLikeScore;
        }

        @Override
        public int compareTo(Client o) {
            return Comparators.SCORE.compare(this, o);
        }

        public static class Comparators {
            public static final Comparator<Client> SCORE = Comparator.comparingInt(Client::getDisLikeScore);
        }

        @Override
        public String toString() {
            return "Client{" +
                    "likes=" + likes +
                    ", dislikes=" + dislikes +
                    ", disLikeScore=" + disLikeScore +
                    '}';
        }
    }
}
