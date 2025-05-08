package com.projetointegrador.seumentor.tutoring.service;

import com.projetointegrador.seumentor.tutoring.api.TutoringQuery;
import com.projetointegrador.seumentor.tutoring.api.dto.TutoringParticipantInfo;
import com.projetointegrador.seumentor.tutoring.api.dto.TutoringRatingRepresentation;
import com.projetointegrador.seumentor.tutoring.api.dto.TutoringRepresentation;
import com.projetointegrador.seumentor.tutoring.model.StatusTutoring;
import com.projetointegrador.seumentor.tutoring.model.Tutoring;
import com.projetointegrador.seumentor.tutoring.model.TutoringRating;
import com.projetointegrador.seumentor.tutoring.repository.TutoringParticipantsRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRatingRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRepository;
import com.projetointegrador.seumentor.user.api.UserAvailabilityFinder;
import com.projetointegrador.seumentor.user.model.DayWeek;
import com.projetointegrador.seumentor.user.model.MentorAvailability;
import com.projetointegrador.seumentor.user.model.User;
import com.projetointegrador.seumentor.tutoring.model.TutoringParticipants;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TutoringQueryAdapter implements TutoringQuery {

    private final TutoringRepository tutoringRepository;
    private final TutoringRatingRepository tutoringRatingRepository;
    private final TutoringParticipantsRepository tutoringParticipantsRepository;
    private UserAvailabilityFinder userAvailabilityFinder; // Make it non-final

    private static final Logger log = LoggerFactory.getLogger(TutoringQueryAdapter.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    public TutoringQueryAdapter(TutoringRepository tutoringRepository,
                                TutoringRatingRepository tutoringRatingRepository,
                                TutoringParticipantsRepository tutoringParticipantsRepository) {
        this.tutoringRepository = tutoringRepository;
        this.tutoringRatingRepository = tutoringRatingRepository;
        this.tutoringParticipantsRepository = tutoringParticipantsRepository;
    }

    @Autowired
    @Lazy
    public void setUserAvailabilityFinder(UserAvailabilityFinder userAvailabilityFinder) {
        this.userAvailabilityFinder = userAvailabilityFinder;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsTutoringForDiscipline(Long disciplineId) {
        return tutoringRepository.existsByDisciplineId(disciplineId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TutoringRepresentation> findTutoringById(Long tutoringId) {
        log.debug("Attempting to find Tutoring by ID: {}", tutoringId);
        return tutoringRepository.findById(tutoringId)
                .map(this::mapToRepresentation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutoringRepresentation> findFilteredTutorings(Long mentorId, Long disciplineId, StatusTutoring status) {
        log.debug("Finding Tutorings with filters - MentorId: {}, DisciplineId: {}, Status: {}", mentorId, disciplineId, status);

        Specification<Tutoring> spec = Specification.where(null);

        if (mentorId != null) {
            spec = spec.and(TutoringSpecifications.withMentorId(mentorId));
        }
        if (disciplineId != null) {
            spec = spec.and(TutoringSpecifications.withDisciplineId(disciplineId));
        }
        if (status != null) {
            spec = spec.and(TutoringSpecifications.withStatus(status));
        }

        List<Tutoring> tutorings = tutoringRepository.findAll(spec);

        log.debug("Found {} tutorings matching criteria.", tutorings.size());

        return tutorings.stream()
                .map(this::mapToRepresentation)
                .collect(Collectors.toList());
    }

    static class TutoringSpecifications {

        public static Specification<Tutoring> withMentorId(Long mentorId) {
            return (root, query, criteriaBuilder) -> {
                return criteriaBuilder.equal(root.get("mentor").get("id"), mentorId);
            };
        }

        public static Specification<Tutoring> withDisciplineId(Long disciplineId) {
            return (root, query, criteriaBuilder) -> {
                return criteriaBuilder.equal(root.get("discipline").get("id"), disciplineId);
            };
        }

        public static Specification<Tutoring> withStatus(StatusTutoring status) {
            return (root, query, criteriaBuilder) -> {
                return criteriaBuilder.equal(root.get("status"), status);
            };
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutoringRepresentation> findAllTutoringsByMentorId(Long mentorId) {
        log.debug("TutoringQueryAdapter: Finding all tutorings for mentor ID: {}", mentorId);
        if (mentorId == null) {
            return Collections.emptyList();
        }
        Specification<Tutoring> spec = TutoringSpecifications.withMentorId(mentorId);
        List<Tutoring> tutorings = tutoringRepository.findAll(spec);
        return tutorings.stream()
                .map(this::mapToRepresentation)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutoringRepresentation> findAllTutoringsByParticipantId(Long userId) {
        log.debug("TutoringQueryAdapter: Finding all tutorings for participant (user) ID: {}", userId);
        if (userId == null) {
            return Collections.emptyList();
        }

        List<TutoringParticipants> participations = tutoringParticipantsRepository.findByUserId(userId);

        return participations.stream()
                .map(TutoringParticipants::getTutoring)
                .filter(Objects::nonNull)
                .distinct()
                .map(this::mapToRepresentation)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional(readOnly = true)
    public List<TutoringRatingRepresentation> findAllTutoringRatings() {
        log.debug("Attempting to find all tutoring ratings.");
        List<TutoringRating> ratings = tutoringRatingRepository.findAll();
        log.info("Found {} tutoring ratings.", ratings.size());

        return ratings.stream()
                .map(rating -> {
                    Long raterUserId = null;
                    if (rating.getTutoring() != null) {
                        Tutoring tutoring = rating.getTutoring();
                        User mentor = tutoring.getMentor();
                        if (mentor != null && tutoring.getTopics() != null) {
                            Optional<TutoringParticipants> raterParticipant = tutoring.getTopics().stream()
                                    .filter(p -> p.getUser() != null && !p.getUser().getId().equals(mentor.getId()))
                                    .findFirst();
                            if (raterParticipant.isPresent()) {
                                raterUserId = raterParticipant.get().getUser().getId();
                            } else {
                                log.warn("Could not determine rater for rating ID {} (Tutoring ID {}). No non-mentor participant found.",
                                        rating.getId(), tutoring.getId());
                            }
                        } else {
                            log.warn("Could not determine rater for rating ID {}. Mentor or Topics are null for Tutoring ID {}.",
                                    rating.getId(), tutoring.getId());
                        }
                    } else {
                        log.warn("Could not determine rater for rating ID {}. Associated tutoring is null.", rating.getId());
                    }
                    return this.mapToRatingRepresentation(rating, raterUserId);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutoringRepresentation> findTutoringAndAvailabilityByDate(LocalDate date) {
        log.info("Adapter: Finding Tutorings and Availabilities for date: {}", date);
        List<TutoringRepresentation> results = new ArrayList<>();

        Specification<Tutoring> specTutoring = (root, query, cb) -> cb.equal(root.get("tutoringDate"), date);
        List<Tutoring> tutoringsOnDate = tutoringRepository.findAll(specTutoring);
        results.addAll(tutoringsOnDate.stream()
                .map(this::mapToRepresentation)
                .collect(Collectors.toList()));

        if (this.userAvailabilityFinder == null) {
            log.error("UserAvailabilityFinder is not injected in TutoringQueryAdapter");
            throw new IllegalStateException("UserAvailabilityFinder service not available");
        }

        List<MentorAvailability> availabilitiesOnDay = findAvailabilitiesForDayOfWeek(date);
        log.debug("Adapter: Found {} MentorAvailabilities via UserAvailabilityFinder for the day of week of {}", availabilitiesOnDay.size(), date);

        results.addAll(availabilitiesOnDay.stream()
                .filter(avail -> Boolean.TRUE.equals(avail.getIsAvailable()))
                .map(avail -> mapAvailabilityToTutoringRepresentation(avail, date))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList()));

        log.info("Adapter: Returning {} combined Tutorings/Availabilities for date {}", results.size(), date);
        return results;
    }

    private List<MentorAvailability> findAvailabilitiesForDayOfWeek(LocalDate date) {
        try {
            DayWeek dayOfWeekEnum = mapJavaDayOfWeekToDayWeekEnum(date.getDayOfWeek());
            log.debug("Adapter: Querying UserAvailabilityFinder.findAllAvailabilitiesByDayOfWeek for: {}", dayOfWeekEnum);
            if (this.userAvailabilityFinder == null) {
                log.error("UserAvailabilityFinder is null in findAvailabilitiesForDayOfWeek");
                return Collections.emptyList();
            }
            return userAvailabilityFinder.findAllAvailabilitiesByDayOfWeek(dayOfWeekEnum);
        } catch (IllegalArgumentException e) {
            log.error("Adapter: Could not map Java DayOfWeek {} to DayWeek enum.", date.getDayOfWeek(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Adapter: Error calling UserAvailabilityFinder.findAllAvailabilitiesByDayOfWeek for {}: {}", date, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutoringRepresentation> findAvailableSlotsForUser(
            LocalDate date,
            Optional<Long> disciplineId,
            Long requestingUserId) {

        if (this.userAvailabilityFinder == null) {
            log.error("UserAvailabilityFinder is not injected in TutoringQueryAdapter for findAvailableSlotsForUser");
            throw new IllegalStateException("UserAvailabilityFinder service not available");
        }

        Specification<Tutoring> specConcrete = buildTutoringSpecificationForAvailableSlots(date, disciplineId, requestingUserId);
        List<Tutoring> concreteTutoringsFromOthers = tutoringRepository.findAll(specConcrete);

        List<TutoringRepresentation> availableSlots = concreteTutoringsFromOthers.stream()
                .filter(tutoring -> isTutoringAvailableForUser(tutoring, requestingUserId))
                .map(this::mapToRepresentation)
                .collect(Collectors.toCollection(ArrayList::new));

        Set<String> concreteTutoringKeys = generateConcreteTutoringKeys(concreteTutoringsFromOthers);

        List<MentorAvailability> allAvailabilitiesOnDay = findAvailabilitiesForDayOfWeek(date);
        log.debug("Adapter: Found {} total MentorAvailabilities via UserAvailabilityFinder for the day of week of {}",
                allAvailabilitiesOnDay.size(), date);

        allAvailabilitiesOnDay.stream()
                .filter(avail -> isAvailabilityRelevantForSlots(avail, requestingUserId, disciplineId))
                .filter(avail -> !isAvailabilitySuperseded(avail, concreteTutoringKeys))
                .map(avail -> mapAvailabilityToTutoringRepresentation(avail, date))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(availableSlots::add);

        availableSlots.sort(Comparator.comparing(TutoringRepresentation::startTime, Comparator.nullsLast(String::compareTo)));
        return availableSlots;
    }


    private Specification<Tutoring> buildTutoringSpecificationForAvailableSlots(LocalDate date, Optional<Long> disciplineId, Long requestingUserId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tutoringDate"), date));
            predicates.add(cb.notEqual(root.get("mentor").get("id"), requestingUserId));
            disciplineId.ifPresent(discId -> predicates.add(cb.equal(root.get("discipline").get("id"), discId)));
            predicates.add(root.get("status").in(StatusTutoring.AGENDADA));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private boolean isTutoringAvailableForUser(Tutoring tutoring, Long requestingUserId) {
        if (tutoring == null || requestingUserId == null || tutoring.getStatus() != StatusTutoring.AGENDADA) {
            return false;
        }
        boolean isFull = tutoring.getMaxParticipants() != null &&
                tutoring.getTopics() != null &&
                tutoring.getTopics().size() >= tutoring.getMaxParticipants();
        if (isFull) return false;

        boolean isParticipating = tutoring.getTopics() != null &&
                tutoring.getTopics().stream()
                        .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(requestingUserId));
        return !isParticipating;
    }

    private Set<String> generateConcreteTutoringKeys(List<Tutoring> tutorings) {
        return tutorings.stream()
                .map(tutoring -> {
                    Long mentorIdKey = tutoring.getMentor() != null ? tutoring.getMentor().getId() : -1L;
                    Long disciplineIdKey = tutoring.getDiscipline() != null ? tutoring.getDiscipline().getId() : -1L;
                    String startTimeStr = formatTime(tutoring.getStartTime());
                    String endTimeStr = formatTime(tutoring.getEndTime());
                    startTimeStr = startTimeStr == null ? "null" : startTimeStr;
                    endTimeStr = endTimeStr == null ? "null" : endTimeStr;
                    return String.format("%d-%d-%s-%s", mentorIdKey, disciplineIdKey, startTimeStr, endTimeStr);
                })
                .collect(Collectors.toSet());
    }


    private boolean isAvailabilityRelevantForSlots(MentorAvailability avail, Long requestingUserId, Optional<Long> disciplineId) {
        if (!Boolean.TRUE.equals(avail.getIsAvailable())) return false;
        if (avail.getUser() == null || avail.getUser().getId().equals(requestingUserId)) return false;
        if (disciplineId.isPresent()) {
            if (avail.getDiscipline() == null || !avail.getDiscipline().getId().equals(disciplineId.get())) {
                return false;
            }
        }

        if (avail.getStartTime() == null || avail.getEndTime() == null) {
            log.warn("Disponibilidade ID {} com tempo de início ou fim nulo encontrada.", avail.getId());
            return false;
        }
        return true;
    }

    private boolean isAvailabilitySuperseded(MentorAvailability avail, Set<String> concreteTutoringKeys) {
        Long availMentorId = avail.getUser() != null ? avail.getUser().getId() : -1L;
        Long availDisciplineId = avail.getDiscipline() != null ? avail.getDiscipline().getId() : -1L;
        String availStartTimeStr = formatTime(avail.getStartTime());
        String availEndTimeStr = formatTime(avail.getEndTime());

        availStartTimeStr = availStartTimeStr == null ? "null" : availStartTimeStr;
        availEndTimeStr = availEndTimeStr == null ? "null" : availEndTimeStr;

        String availabilityKey = String.format("%d-%d-%s-%s",
                availMentorId, availDisciplineId, availStartTimeStr, availEndTimeStr);

        boolean isSuperseded = concreteTutoringKeys.contains(availabilityKey);
        if (isSuperseded) {
            log.trace("Adapter: Availability (Key: {}) is superseded.", availabilityKey);
        }
        return isSuperseded;
    }



    private DayWeek mapJavaDayOfWeekToDayWeekEnum(java.time.DayOfWeek javaDayOfWeek) {
        return switch (javaDayOfWeek) {
            case MONDAY -> DayWeek.SEGUNDA_FEIRA;
            case TUESDAY -> DayWeek.TERCA_FEIRA;
            case WEDNESDAY -> DayWeek.QUARTA_FEIRA;
            case THURSDAY -> DayWeek.QUINTA_FEIRA;
            case FRIDAY -> DayWeek.SEXTA_FEIRA;
            case SATURDAY -> DayWeek.SABADO;
            case SUNDAY -> DayWeek.DOMINGO;
        };
    }

    private String formatTime(java.time.LocalTime time) {
        if (TIME_FORMATTER == null) {
            log.error("DateTimeFormatter TIME_FORMATTER não inicializado!");
            return time != null ? time.toString() : null;
        }
        return time != null ? time.format(TIME_FORMATTER) : null;
    }

    private String formatDate(LocalDate date) {
        if (DATE_FORMATTER == null) {
            log.error("DateTimeFormatter DATE_FORMATTER não inicializado!");
            return date != null ? date.toString() : null;
        }
        return date != null ? date.format(DATE_FORMATTER) : null;
    }

    private Optional<TutoringRepresentation> mapAvailabilityToTutoringRepresentation(MentorAvailability availability, LocalDate date) {
        if (availability == null) {
            log.warn("Attempted to map a null MentorAvailability.");
            return Optional.empty();
        }
        User mentor = availability.getUser();
        com.projetointegrador.seumentor.course.model.Discipline discipline = availability.getDiscipline();

        if (mentor == null || discipline == null) {
            log.warn("Skipping mapping availability ID {} due to null User or Discipline.", availability.getId());
            return Optional.empty();
        }

        String formattedStartTime = availability.getStartTime() != null ? availability.getStartTime().format(TIME_FORMATTER) : null;
        String formattedEndTime = availability.getEndTime() != null ? availability.getEndTime().format(TIME_FORMATTER) : null;
        String formattedDate = date != null ? date.format(DATE_FORMATTER) : null;

        if (formattedStartTime == null || formattedEndTime == null || formattedDate == null) {
            log.warn("Skipping mapping availability ID {} due to null start/end time or date.", availability.getId());
            return Optional.empty();
        }

        TutoringRepresentation rep = new TutoringRepresentation(
                null,
                mentor.getId(),
                mentor.getFirstName() + " " + mentor.getLastName(),
                discipline.getId(),
                discipline.getDisciplineName(),
                availability.getTutoringClassType(),
                StatusTutoring.A_MARCAR,
                formattedStartTime,
                formattedEndTime,
                formattedDate,
                null,
                null,
                null,
                null, // isChatEnable - not applicable for availability
                new HashSet<>()
        );
        return Optional.of(rep);
    }


    private TutoringRepresentation mapToRepresentation(Tutoring tutoring) {
        if (tutoring == null) {
            return null;
        }

        String mentorName = "[Mentor Inválido]";
        Long mentorId = null;
        if (tutoring.getMentor() != null) {
            try {
                mentorId = tutoring.getMentor().getId(); // Eager fetch or already loaded
                mentorName = tutoring.getMentor().getFirstName() + " " + tutoring.getMentor().getLastName();
            } catch (EntityNotFoundException e) {
                log.warn("Mentor associated with tutoring {} not found during mapping.", tutoring.getId());
            }
        }

        String disciplineName = "[Disciplina Inválida]";
        Long disciplineId = null;
        if (tutoring.getDiscipline() != null) {
            try {
                disciplineId = tutoring.getDiscipline().getId(); // Eager fetch or already loaded
                disciplineName = tutoring.getDiscipline().getDisciplineName();
            } catch (EntityNotFoundException e) {
                log.warn("Discipline associated with tutoring {} not found during mapping.", tutoring.getId());
            }
        }

        Set<TutoringParticipantInfo> participantsInfo = new HashSet<>();
        if (tutoring.getTopics() != null) {
            participantsInfo = tutoring.getTopics().stream()
                    .map(p -> {
                        String participantName = "[Participante Inválido]";
                        Long participantId = null;
                        if (p.getUser() != null) {
                            try {
                                participantId = p.getUser().getId();
                                participantName = p.getUser().getFirstName() + " " + p.getUser().getLastName();
                            } catch (EntityNotFoundException e) {
                                log.warn("Participant user associated with tutoring {} not found during mapping.", tutoring.getId());
                            }
                        }
                        return new TutoringParticipantInfo(participantId, participantName, p.getTopic());
                    })
                    .collect(Collectors.toSet());
        }

        String formattedStartTime = null;
        if (tutoring.getStartTime() != null) {
            formattedStartTime = tutoring.getStartTime().format(TIME_FORMATTER);
        }

        String formattedEndTime = null;
        if (tutoring.getEndTime() != null) {
            formattedEndTime = tutoring.getEndTime().format(TIME_FORMATTER);
        }

        String formattedTutoringDate = null;
        if (tutoring.getTutoringDate() != null) {
            formattedTutoringDate = tutoring.getTutoringDate().format(DATE_FORMATTER);
        }

        return new TutoringRepresentation(
                tutoring.getId(),
                mentorId,
                mentorName,
                disciplineId,
                disciplineName,
                tutoring.getTutoringClassType(),
                tutoring.getStatus(),
                formattedStartTime,
                formattedEndTime,
                formattedTutoringDate,
                tutoring.getLocal(),
                tutoring.getLinkVideo(),
                tutoring.getMaxParticipants(),
                tutoring.getIsChatEnable(),
                participantsInfo
        );
    }
    @Override
    public TutoringRatingRepresentation mapToRatingRepresentation(TutoringRating rating, Long raterUserId) {
        if (rating == null) {
            return null;
        }
        return new TutoringRatingRepresentation(
                rating.getId(),
                rating.getTutoring() != null ? rating.getTutoring().getId() : null,
                raterUserId, // This was passed as a parameter
                rating.getMentorRating(),
                rating.getReview(),
                rating.getCreatedAt(),
                rating.getUpdatedAt()
        );
    }
}