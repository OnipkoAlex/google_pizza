import java.util.List;
import java.util.Objects;

public class CountSatisfied {

    public static Integer countSatisfiedClients(String result, List<PizzaMain.Client> clientsList) {
        Integer count = 0;
        int likeCount;
        String[] output;
        boolean found = false;

        output = result.substring(2).split(" ");
        System.out.println(clientsList.size() + " clients");

        for (PizzaMain.Client client : clientsList) {
            likeCount = 0;
            for (String likeIngredient : client.getLikes()) {
                for (String s : output) {
                    if (Objects.equals(likeIngredient, s)) {
                        likeCount++;
                    }
                }
            }
            for (String dislikeIngredient : client.getDislikes()) {
                for (String s : output) {
                    if (Objects.equals(dislikeIngredient, s)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found && likeCount == client.getLikes().size()) {
                count++;
            } else {
                found = false;
            }
        }

        return count;
    }
}
