package com.openclassroom.safetynet.repository;

import com.openclassroom.safetynet.model.Person;
import com.openclassroom.safetynet.service.FileIOService;
import com.openclassroom.safetynet.utils.ConvertToUpper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // Ajout import manquant
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository responsable de l'accès et de la manipulation des données des personnes (Person).
 * <p>
 * Ce repository interagit avec le {@link FileIOService} pour obtenir la liste actuelle des personnes
 * et pour persister les modifications (via {@link FileIOService#setPersons(List)}).
 * Il fournit des méthodes pour rechercher des personnes par différents critères (adresse, nom, ville, etc.)
 * et pour effectuer les opérations CRUD de base sur les personnes. L'unicité d'une personne est
 * généralement définie par la combinaison prénom/nom.
 * </p>
 */
@Repository
public class PersonRepository {

    private static final Logger logger = LoggerFactory.getLogger(PersonRepository.class);
    private final FileIOService fileIOService;

    /**
     * Construit une nouvelle instance de PersonRepository.
     *
     * @param fileIOService Le service injecté responsable de l'accès aux données brutes du fichier.
     */
    @Autowired // Optionnel ici
    public PersonRepository(FileIOService fileIOService) {
        this.fileIOService = fileIOService;
    }

    /**
     * Trouve toutes les personnes résidant à une ou plusieurs adresses spécifiées dans une liste.
     * <p>
     * La comparaison des adresses est effectuée de manière <b>insensible à la casse</b>,
     * après conversion en majuscules des adresses recherchées et des adresses des personnes stockées.
     * Utilise {@link ConvertToUpper#convertList(List)} pour préparer la liste des adresses recherchées.
     * </p>
     *
     * @param addresses La liste des adresses ({@code List<String>}) à rechercher.
     *                  Peut être {@code null} ou vide.
     * @return Une {@code List<Person>} contenant toutes les personnes trouvées résidant à l'une des
     *         adresses spécifiées. Retourne une liste vide si aucune personne n'est trouvée,
     *         si la liste d'adresses est {@code null} ou vide, ou si la source de données est vide.
     */
    public List<Person> findByAddressIn(List<String> addresses) {
        // Retourner immédiatement si l'entrée est invalide
        if (addresses == null || addresses.isEmpty()) {
            logger.trace("findByAddressIn: Liste d'adresses nulle ou vide, retour d'une liste vide.");
            return Collections.emptyList();
        }

        // Obtient la liste actuelle des personnes
        List<Person> currentPersons = fileIOService.getPersons();
        if (currentPersons.isEmpty()) {
            logger.trace("findByAddressIn: Liste de personnes source vide, retour d'une liste vide.");
            return Collections.emptyList();
        }

        // Convertir les adresses recherchées en majuscules (ne modifie pas la liste originale)
        List<String> addressesUpper = ConvertToUpper.convertList(addresses);

        // Filtrer les personnes dont l'adresse (convertie en majuscule) est contenue dans la liste des adresses recherchées
        return currentPersons.stream()
                // Vérifier la nullité de l'adresse de la personne avant d'appeler toUpperCase()
                .filter(p -> p.getAddress() != null && addressesUpper.contains(p.getAddress().toUpperCase()))
                .collect(Collectors.toList());
    }

    /**
     * Trouve une personne spécifique par son prénom et son nom de famille.
     * <p>
     * La recherche est effectuée de manière <b>sensible à la casse</b> et après suppression
     * des espaces de début/fin (trim) sur le prénom et le nom fournis.
     * On suppose qu'il n'existe qu'une seule personne par combinaison unique de prénom et nom exacts.
     * </p>
     *
     * @param firstName Le prénom exact (sensible à la casse, trimé) de la personne.
     * @param lastName Le nom de famille exact (sensible à la casse, trimé) de la personne.
     * @return Un {@link Optional<Person>} contenant la personne si elle est trouvée,
     *         sinon {@link Optional#empty()}. Retourne également {@link Optional#empty()} si
     *         le prénom ou le nom fourni est {@code null}.
     */
    public Optional<Person> findByFirstNameAndLastName(String firstName, String lastName) {
        // Validation rapide
        if (firstName == null || lastName == null) {
            return Optional.empty();
        }
        // Nettoyage des entrées
        String firstNameTrimmed = firstName.trim();
        String lastNameTrimmed = lastName.trim();

        List<Person> currentPersons = fileIOService.getPersons();

        // Recherche exacte et sensible à la casse après trim
        return currentPersons.stream()
                .filter(p -> Objects.equals(p.getFirstName(), firstNameTrimmed) && Objects.equals(p.getLastName(), lastNameTrimmed))
                .findFirst();
    }

    /**
     * Trouve toutes les personnes résidant à une adresse spécifique.
     * <p>
     * La recherche de l'adresse est <b>sensible à la casse</b> et exacte.
     * </p>
     *
     * @param address L'adresse exacte (sensible à la casse) à rechercher.
     * @return Une {@code List<Person>} contenant toutes les personnes trouvées à cette adresse.
     *         Retourne une liste vide si aucune personne n'est trouvée ou si l'adresse est {@code null}.
     */
    public List<Person> findByAddress(String address) {
        if (address == null) {
            return Collections.emptyList();
        }
        List<Person> currentPersons = fileIOService.getPersons();
        // Recherche exacte, sensible à la casse
        return currentPersons.stream()
                .filter(p -> Objects.equals(p.getAddress(), address))
                .collect(Collectors.toList());
    }

    /**
     * Récupère la liste complète de toutes les personnes enregistrées.
     * Retourne une nouvelle copie mutable de la liste obtenue depuis {@link FileIOService}.
     *
     * @return Une {@code List<Person>} contenant toutes les personnes.
     *         Cette liste est une copie mutable et peut être vide.
     */
    public List<Person> findAll() {
        // Crée une copie mutable
        return new ArrayList<>(fileIOService.getPersons());
    }

    /**
     * Trouve toutes les personnes ayant un nom de famille spécifique.
     * <p>
     * La recherche est <b>insensible à la casse</b> et ignore les espaces de début/fin (trim)
     * pour le nom de famille fourni.
     * </p>
     *
     * @param lastName Le nom de famille (insensible à la casse, trimé) à rechercher.
     * @return Une {@code List<Person>} contenant toutes les personnes trouvées avec ce nom de famille.
     *         Retourne une liste vide si aucune personne n'est trouvée ou si le nom est {@code null}.
     */
    public List<Person> findByLastName(String lastName) {
        if (lastName == null) {
            return Collections.emptyList();
        }
        String lastNameTrimmed = lastName.trim();
        List<Person> currentPersons = fileIOService.getPersons();
        // Recherche insensible à la casse
        return currentPersons.stream()
                .filter(p -> p.getLastName() != null && p.getLastName().equalsIgnoreCase(lastNameTrimmed))
                .collect(Collectors.toList());
    }

    /**
     * Trouve toutes les personnes résidant dans une ville spécifique.
     * <p>
     * La recherche est <b>insensible à la casse</b> pour le nom de la ville fourni.
     * </p>
     *
     * @param city Le nom de la ville (insensible à la casse) à rechercher.
     * @return Une {@code List<Person>} contenant toutes les personnes trouvées dans cette ville.
     *         Retourne une liste vide si aucune personne n'est trouvée ou si la ville est {@code null}.
     */
    public List<Person> findByCity(String city) {
        if (city == null) {
            return Collections.emptyList();
        }
        List<Person> currentPersons = fileIOService.getPersons();
        // Recherche insensible à la casse
        return currentPersons.stream()
                .filter(p -> p.getCity() != null && p.getCity().equalsIgnoreCase(city)) // Pas de trim ici, vérifier si nécessaire
                .collect(Collectors.toList());
    }

    /**
     * Sauvegarde (ajoute ou met à jour) une personne.
     * <p>
     * Si une personne avec le même prénom et nom existe déjà (comparaison insensible à la casse),
     * elle est remplacée par les nouvelles informations fournies dans l'objet {@code person}.
     * Sinon, la nouvelle personne est ajoutée à la liste.
     * </p><p>
     * L'objet {@code person} fourni est ajouté directement à la liste après suppression de l'éventuel
     * doublon. Aucun nettoyage (trim) n'est effectué sur les champs de l'objet {@code person} par cette méthode.
     * </p><p>
     * Après modification de la liste en mémoire, la liste complète est passée à
     * {@link FileIOService#setPersons(List)} pour persistance.
     * </p>
     *
     * @param person L'objet {@link Person} à sauvegarder. Ne devrait pas être {@code null}.
     *               Le prénom et le nom ne devraient pas être nuls pour la logique de remplacement.
     * @return L'objet {@link Person} qui a été passé en argument (et donc sauvegardé).
     * @throws NullPointerException si {@code person} ou son prénom/nom sont {@code null} lors de l'appel à `equalsIgnoreCase`.
     *                              Il est recommandé que l'appelant valide l'objet Person avant d'appeler save.
     */
    public Person save(Person person) {
        // Note: Pas de validation d'entrée ici. Risque de NPE si person ou ses identifiants sont null.
        // La validation devrait idéalement être faite dans le service appelant.

        // Obtenir une copie mutable
        ArrayList<Person> persons = new ArrayList<>(fileIOService.getPersons());

        // Supprimer l'ancien enregistrement si existant (insensible à la casse)
        // Attention: Le NPE peut survenir ici si person.getFirstName() ou person.getLastName() est null
        boolean removed = persons.removeIf(p -> p.getFirstName() != null && p.getLastName() != null && // Vérif null côté liste
                p.getFirstName().equalsIgnoreCase(person.getFirstName()) && // Risque NPE si person.getFirstName() est null
                p.getLastName().equalsIgnoreCase(person.getLastName()));    // Risque NPE si person.getLastName() est null
        if(removed){
            logger.debug("Personne {} {} existante supprimée avant sauvegarde.", person.getFirstName(), person.getLastName());
        }

        // Ajouter la nouvelle personne (ou la version mise à jour)
        persons.add(person);
        logger.debug("Personne {} {} ajoutée/mise à jour dans la liste.", person.getFirstName(), person.getLastName());


        // Persister la liste modifiée
        fileIOService.setPersons(persons);

        // Retourner l'objet tel qu'il a été passé en argument
        return person;
    }

    /**
     * Supprime une personne identifiée par son prénom et son nom de famille.
     * <p>
     * La recherche de la personne à supprimer est effectuée de manière <b>insensible à la casse</b>
     * et ignore les espaces de début/fin (trim) pour le prénom et le nom fournis.
     * </p><p>
     * Si une personne est trouvée et supprimée, la liste mise à jour est persistée via
     * {@link FileIOService#setPersons(List)}.
     * </p>
     *
     * @param firstName Le prénom (insensible à la casse, trimé) de la personne à supprimer.
     * @param lastName Le nom de famille (insensible à la casse, trimé) de la personne à supprimer.
     * @return {@code true} si une personne a été trouvée et supprimée, {@code false} sinon (y compris
     *         si le prénom ou le nom fourni est nul ou vide).
     */
    public boolean deleteByFirstNameAndLastName(String firstName, String lastName) {
        // Validation et nettoyage
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            logger.debug("Tentative de suppression avec prénom ou nom nul ou vide.");
            return false;
        }
        String firstNameTrimmed = firstName.trim();
        String lastNameTrimmed = lastName.trim(); // Trim aussi le nom ici

        // Obtenir une copie mutable
        ArrayList<Person> persons = new ArrayList<>(fileIOService.getPersons());

        // Tenter la suppression (insensible à la casse)
        boolean removed = persons.removeIf(p -> p.getFirstName() != null && p.getLastName() != null && // Vérif null côté liste
                p.getFirstName().equalsIgnoreCase(firstNameTrimmed) &&
                p.getLastName().equalsIgnoreCase(lastNameTrimmed));

        // Si supprimé, persister
        if (removed) {
            fileIOService.setPersons(persons);
            logger.info("Personne {} {} supprimée.", firstNameTrimmed, lastNameTrimmed);
        } else {
            logger.warn("Personne {} {} non trouvée pour suppression.", firstNameTrimmed, lastNameTrimmed);
        }
        return removed;
    }

    /**
     * Vérifie si une personne existe pour une combinaison donnée de prénom et nom.
     * <p>
     * La vérification est effectuée de manière <b>insensible à la casse</b> et ignore les espaces
     * de début/fin (trim) pour le prénom et le nom fournis.
     * </p>
     *
     * @param firstName Le prénom (insensible à la casse, trimé) à vérifier.
     * @param lastName Le nom de famille (insensible à la casse, trimé) à vérifier.
     * @return {@code true} si une personne existe pour cette combinaison, {@code false} sinon (y compris
     *         si le prénom ou le nom fourni est nul ou vide).
     */
    public boolean existsById(String firstName, String lastName) {
        // Validation et nettoyage
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            logger.trace("existsById: Prénom ou nom nul/vide -> false");
            return false;
        }
        String firstNameTrimmed = firstName.trim();
        String lastNameTrimmed = lastName.trim();

        List<Person> currentPersons = fileIOService.getPersons();

        // Recherche insensible à la casse
        return currentPersons.stream()
                .anyMatch(p -> p.getFirstName() != null && p.getLastName() != null && // Vérif null côté liste
                        p.getFirstName().equalsIgnoreCase(firstNameTrimmed) &&
                        p.getLastName().equalsIgnoreCase(lastNameTrimmed));
    }
}