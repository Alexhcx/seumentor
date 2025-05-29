package com.projetointegrador.seumentor.tutoring.service;

import com.projetointegrador.seumentor.common.enums.DayWeek;
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.tutoring.api.TutoringQuery;
import com.projetointegrador.seumentor.tutoring.api.UserAvailabilityFinder;
import com.projetointegrador.seumentor.tutoring.api.dto.*;
import com.projetointegrador.seumentor.tutoring.api.mapper.TutoringMapper;
import com.projetointegrador.seumentor.tutoring.enums.StatusTutoring;
import com.projetointegrador.seumentor.tutoring.exception.TutoringNotFoundException;
import com.projetointegrador.seumentor.tutoring.model.MentorAvailability;
import com.projetointegrador.seumentor.tutoring.model.Tutoring;
import com.projetointegrador.seumentor.tutoring.model.TutoringParticipants;
import com.projetointegrador.seumentor.tutoring.model.TutoringRating;
import com.projetointegrador.seumentor.tutoring.repository.MentorAvailabilityRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringParticipantsRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRatingRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRepository;
import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.user.model.User;

import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TutoringQueryAdapter implements TutoringQuery, UserAvailabilityFinder {

        private final TutoringRepository tutoringRepository;
        private final TutoringRatingRepository tutoringRatingRepository;
        private final TutoringParticipantsRepository tutoringParticipantsRepository;
        private final MentorAvailabilityRepository mentorAvailabilityRepository;
        private final TutoringMapper tutoringMapper;
        private final UserQuery userQuery;

        private static final Logger log = LoggerFactory.getLogger(TutoringQueryAdapter.class);

        @Autowired
        public TutoringQueryAdapter(TutoringRepository tutoringRepository,
                        TutoringRatingRepository tutoringRatingRepository,
                        TutoringParticipantsRepository tutoringParticipantsRepository,
                        MentorAvailabilityRepository mentorAvailabilityRepository,
                        TutoringMapper tutoringMapper,
                        UserQuery userQuery) {
                this.tutoringRepository = tutoringRepository;
                this.tutoringRatingRepository = tutoringRatingRepository;
                this.tutoringParticipantsRepository = tutoringParticipantsRepository;
                this.mentorAvailabilityRepository = mentorAvailabilityRepository;
                this.tutoringMapper = tutoringMapper;
                this.userQuery = userQuery;
        }

        @Override
        @Transactional(readOnly = true)
        public boolean existsTutoringForDiscipline(Long disciplineId) {
                return tutoringRepository.existsByDisciplineId(disciplineId);
        }

        private TutoringRepresentation enrichTutoringRepresentation(Tutoring tutoring) {
                if (tutoring == null)
                        return null;
                TutoringRepresentation rep = tutoringMapper.toTutoringRepresentation(tutoring);
                if (rep != null) {
                        int qtdParticipants = tutoringParticipantsRepository.countByTutoringId(tutoring.getId());
                        return new TutoringRepresentation(
                                        rep.id(), rep.mentorId(), rep.mentorName(), rep.disciplineId(),
                                        rep.disciplineName(),
                                        rep.tutoringClassType(), rep.status(), rep.startTime(), rep.endTime(),
                                        rep.tutoringDate(),
                                        rep.local(), rep.linkVideo(), rep.maxParticipants(), qtdParticipants,
                                        rep.isChatEnable(), rep.participants());
                }
                return null;
        }

        @Override
        @Transactional(readOnly = true)
        public Optional<TutoringRepresentation> findTutoringById(Long tutoringId) {
                log.debug("TutoringQueryAdapter: Finding Tutoring by ID: {}", tutoringId);
                return tutoringRepository.findById(tutoringId)
                                .map(this::enrichTutoringRepresentation);
        }

        @Override
        @Transactional(readOnly = true)
        public List<TutoringRepresentation> findFilteredTutorings(Long mentorId, Long disciplineId,
                        StatusTutoring status) {
                log.debug("TutoringQueryAdapter: Finding Tutorings with filters - MentorId: {}, DisciplineId: {}, Status: {}",
                                mentorId, disciplineId, status);
                Specification<Tutoring> spec = TutoringSpecifications.buildSpecification(mentorId, disciplineId,
                                status);
                List<Tutoring> tutorings = tutoringRepository.findAll(spec);
                return tutorings.stream()
                                .map(this::enrichTutoringRepresentation)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
        }

        static class TutoringSpecifications {
                public static Specification<Tutoring> buildSpecification(Long mentorId, Long disciplineId,
                                StatusTutoring status) {
                        return (root, query, criteriaBuilder) -> {
                                List<Predicate> predicates = new ArrayList<>();
                                if (mentorId != null) {
                                        predicates.add(criteriaBuilder.equal(root.get("mentor").get("id"), mentorId));
                                }
                                if (disciplineId != null) {
                                        predicates.add(criteriaBuilder.equal(root.get("discipline").get("id"),
                                                        disciplineId));
                                }
                                if (status != null) {
                                        predicates.add(criteriaBuilder.equal(root.get("status"), status));
                                }
                                if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                                        root.fetch("mentor", jakarta.persistence.criteria.JoinType.LEFT);
                                        root.fetch("discipline", jakarta.persistence.criteria.JoinType.LEFT)
                                                        .fetch("courseArea",
                                                                        jakarta.persistence.criteria.JoinType.LEFT);
                                }
                                query.distinct(true);
                                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
                        };
                }

                public static Specification<Tutoring> buildAvailableSlotsSpecification(LocalDate date,
                                Optional<Long> disciplineId, Long requestingUserId) {
                        return (root, query, cb) -> {
                                List<Predicate> predicates = new ArrayList<>();
                                predicates.add(cb.equal(root.get("tutoringDate"), date));
                                if (requestingUserId != null) {
                                        predicates.add(cb.notEqual(root.get("mentor").get("id"), requestingUserId));
                                }
                                disciplineId.ifPresent(discId -> predicates
                                                .add(cb.equal(root.get("discipline").get("id"), discId)));
                                predicates.add(root.get("status").in(StatusTutoring.AGENDADA, StatusTutoring.PENDENTE));
                                if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                                        root.fetch("mentor", jakarta.persistence.criteria.JoinType.LEFT);
                                        root.fetch("discipline", jakarta.persistence.criteria.JoinType.LEFT)
                                                        .fetch("courseArea",
                                                                        jakarta.persistence.criteria.JoinType.LEFT);
                                }
                                query.distinct(true);
                                return cb.and(predicates.toArray(new Predicate[0]));
                        };
                }
        }

        @Override
        @Transactional(readOnly = true)
        public List<TutoringRepresentation> findAllTutoringsByMentorId(Long mentorId) {
                log.debug("TutoringQueryAdapter: Finding all tutorings for mentor ID: {}", mentorId);
                if (mentorId == null)
                        return Collections.emptyList();
                List<Tutoring> tutorings = tutoringRepository
                                .findAll(TutoringSpecifications.buildSpecification(mentorId, null, null));
                return tutorings.stream()
                                .map(this::enrichTutoringRepresentation)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<TutoringRepresentation> findAllTutoringsByParticipantId(Long userId) {
                log.debug("TutoringQueryAdapter: Finding all tutorings by participant ID: {}", userId);
                if (userId == null)
                        return Collections.emptyList();
                List<TutoringParticipants> participations = tutoringParticipantsRepository.findByUserId(userId);
                return participations.stream()
                                .map(TutoringParticipants::getTutoring)
                                .filter(Objects::nonNull)
                                .distinct()
                                .map(this::enrichTutoringRepresentation)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<TutoringRatingRepresentation> findAllTutoringRatings() {
                log.debug("TutoringQueryAdapter: Finding all tutoring ratings.");
                List<TutoringRating> ratings = tutoringRatingRepository.findAll();
                return ratings.stream()
                                .map(rating -> {
                                        Long raterUserId = null;
                                        if (rating.getTutoring() != null && rating.getTutoring().getTopics() != null
                                                        && !rating.getTutoring().getTopics().isEmpty()) {
                                                User mentor = rating.getTutoring().getMentor();
                                                raterUserId = rating.getTutoring().getTopics().stream()
                                                                .map(TutoringParticipants::getUser)
                                                                .filter(Objects::nonNull)
                                                                .filter(user -> mentor == null || (user.getId() != null
                                                                                && (mentor.getId() == null || !user
                                                                                                .getId()
                                                                                                .equals(mentor.getId()))))
                                                                .map(User::getId)
                                                                .findFirst().orElse(null);
                                        }
                                        return tutoringMapper.toTutoringRatingRepresentation(rating, raterUserId);
                                })
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<TutoringRatingRepresentation> findRatingsByTutoringId(Long tutoringId) {
                log.debug("TutoringQueryAdapter: Finding ratings for tutoring ID: {}", tutoringId);

                Tutoring tutoring = tutoringRepository.findById(tutoringId)
                                .orElseThrow(() -> {
                                        log.warn("TutoringQueryAdapter: Tutoring not found with ID: {} when trying to find ratings.",
                                                        tutoringId);
                                        return new TutoringNotFoundException(
                                                        "Mentoria não encontrada com ID: " + tutoringId);
                                });

                TutoringRating rating = tutoring.getRating();

                if (rating == null) {
                        log.debug("TutoringQueryAdapter: No rating found for tutoring ID: {}", tutoringId);
                        return Collections.emptyList();
                }

                Long raterUserId = null;
                if (rating.getTutoring() != null && rating.getTutoring().getTopics() != null
                                && !rating.getTutoring().getTopics().isEmpty()) {
                        User mentor = rating.getTutoring().getMentor();

                        raterUserId = rating.getTutoring().getTopics().stream()
                                        .map(TutoringParticipants::getUser)
                                        .filter(Objects::nonNull)
                                        .filter(user -> mentor == null
                                                        || (user.getId() != null && (mentor.getId() == null
                                                                        || !user.getId().equals(mentor.getId()))))
                                        .map(User::getId)
                                        .findFirst()
                                        .orElse(null);
                        if (raterUserId == null && rating.getTutoring().getTopics().stream()
                                        .anyMatch(tp -> tp.getUser() != null)) {
                                log.warn("Could not definitively determine a non-mentor rater for tutoring ID {}, rating ID {}. "
                                                +
                                                "This might happen if only the mentor is listed in topics, or data inconsistency.",
                                                tutoringId, rating.getId());
                        }
                } else {
                        log.warn("Tutoring (ID: {}) or its topics are null/empty when trying to determine rater for rating (ID: {}).",
                                        (rating.getTutoring() != null ? rating.getTutoring().getId() : "N/A"),
                                        rating.getId());
                }

                TutoringRatingRepresentation representation = tutoringMapper.toTutoringRatingRepresentation(rating,
                                raterUserId);
                log.debug("TutoringQueryAdapter: Found and mapped rating for tutoring ID: {}", tutoringId);
                return Collections.singletonList(representation);
        }

        @Override
        @Transactional(readOnly = true)
        public Optional<MentorAverageRatingRepresentation> getMentorAverageRating(Long mentorId) {
                log.debug("TutoringQueryAdapter: Calculating average rating for mentor ID: {}", mentorId);

                userQuery.findById(mentorId)
                                .orElseThrow(() -> {
                                        log.warn("TutoringQueryAdapter: Mentor not found with ID: {} when calculating average rating.",
                                                        mentorId);
                                        return new UserNotFoundException("Mentor não encontrado com ID: " + mentorId);
                                });

                // Idealmente, verificar se o usuário tem a role MENTOR, mas para a média,
                // apenas a existência pode ser suficiente
                // se a lógica de criação de tutorias/avaliações já garante que apenas mentores
                // recebem avaliações.

                Specification<Tutoring> spec = TutoringSpecifications.buildSpecification(mentorId, null, null);
                List<Tutoring> mentorTutorings = tutoringRepository.findAll(spec);

                if (mentorTutorings.isEmpty()) {
                        log.debug("TutoringQueryAdapter: No tutorings found for mentor ID: {}. Returning 0 ratings.",
                                        mentorId);
                        return Optional.of(new MentorAverageRatingRepresentation(mentorId, null, 0));
                }

                List<Float> ratings = mentorTutorings.stream()
                                .map(Tutoring::getRating) 
                                .filter(Objects::nonNull) 
                                .map(TutoringRating::getMentorRating) 
                                .filter(Objects::nonNull) 
                                .collect(Collectors.toList());

                if (ratings.isEmpty()) {
                        log.debug("TutoringQueryAdapter: No actual ratings found for mentor ID: {}'s tutorias.",
                                        mentorId);
                        return Optional.of(new MentorAverageRatingRepresentation(mentorId, null, 0));
                }

                double sum = ratings.stream()
                                .mapToDouble(Float::doubleValue) 
                                .sum();
                double average = sum / ratings.size();

                average = Math.round(average * 100.0) / 100.0;

                log.info("TutoringQueryAdapter: Calculated average rating {} for mentor ID {} based on {} ratings.",
                                average, mentorId, ratings.size());
                return Optional.of(new MentorAverageRatingRepresentation(mentorId, average, ratings.size()));
        }

        @Override
        public TutoringRatingRepresentation mapToRatingRepresentation(TutoringRating rating, Long raterUserId) {
                return tutoringMapper.toTutoringRatingRepresentation(rating, raterUserId);
        }

        @Override
        @Transactional(readOnly = true)
        public List<TutoringRepresentation> findTutoringAndAvailabilityByDate(LocalDate date) {
                log.info("TutoringQueryAdapter: Finding Tutorings and Availabilities for date: {}", date);
                List<TutoringRepresentation> results = new ArrayList<>();

                Specification<Tutoring> specTutoring = TutoringSpecifications.buildSpecification(null, null, null)
                                .and((root, query, cb) -> cb.equal(root.get("tutoringDate"), date));
                List<Tutoring> tutoringsOnDate = tutoringRepository.findAll(specTutoring);
                results.addAll(tutoringsOnDate.stream()
                                .map(this::enrichTutoringRepresentation)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()));

                List<MentorAvailability> availabilitiesOnDay = findAllAvailabilitiesByDayOfWeek(
                                mapJavaDayOfWeekToDayWeekEnum(date.getDayOfWeek()));
                results.addAll(availabilitiesOnDay.stream()
                                .filter(avail -> Boolean.TRUE.equals(avail.getIsAvailable()))
                                .map(avail -> tutoringMapper.availabilityToTutoringRepresentation(avail, date))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.toList()));

                results.sort(Comparator.comparing(TutoringRepresentation::startTime,
                                Comparator.nullsLast(String::compareTo)));
                log.info("TutoringQueryAdapter: Returning {} combined Tutorings/Availabilities for date {}",
                                results.size(),
                                date);
                return results;
        }

        @Override
        @Transactional(readOnly = true)
        public List<TutoringRepresentation> findAvailableSlotsForUser(LocalDate date, Optional<Long> disciplineId,
                        Long requestingUserId) {
                log.info("NOVA LÓGICA: Buscando slots para User ID: {} em Data: {}, Disciplina ID: {}",
                                requestingUserId, date, disciplineId.orElse(null));

                LocalDate currentDate = LocalDate.now();
                LocalTime currentTime = LocalTime.now();

                // 1. Buscar TODAS as mentorias concretas (PENDENTE ou AGENDADA) para o slot
                Specification<Tutoring> specConcreteForAllMentors = (root, query, cb) -> {
                        List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
                        predicates.add(cb.equal(root.get("tutoringDate"), date));
                        disciplineId.ifPresent(
                                        discId -> predicates.add(cb.equal(root.get("discipline").get("id"), discId)));
                        predicates.add(root.get("status").in(StatusTutoring.AGENDADA, StatusTutoring.PENDENTE));

                        if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                                root.fetch("mentor", jakarta.persistence.criteria.JoinType.LEFT);
                                root.fetch("discipline", jakarta.persistence.criteria.JoinType.LEFT)
                                                .fetch("courseArea", jakarta.persistence.criteria.JoinType.LEFT);
                        }
                        query.distinct(true);
                        return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
                };

                List<Tutoring> allConcreteTutoringsForSlot = tutoringRepository.findAll(specConcreteForAllMentors);
                log.info("NOVA LÓGICA: Encontradas {} mentorias concretas totais (AGENDADA ou PENDENTE) para o slot (todos os mentores): {}",
                                allConcreteTutoringsForSlot.size(),
                                allConcreteTutoringsForSlot.stream()
                                                .map(t -> String.format(
                                                                "ID: %d, Status: %s, MentorID: %d, Start: %s, End: %s",
                                                                t.getId(), t.getStatus(),
                                                                (t.getMentor() != null ? t.getMentor().getId() : null),
                                                                t.getStartTime(), t.getEndTime()))
                                                .collect(Collectors.toList()));

                List<TutoringRepresentation> resultSlots = new ArrayList<>();
                List<Tutoring> validConcreteTutorings = new ArrayList<>();

                // Filtra mentorias concretas que já passaram (se for o dia atual)
                for (Tutoring tutoring : allConcreteTutoringsForSlot) {
                        boolean include = true;
                        if (date.equals(currentDate)) {
                                if (tutoring.getEndTime() != null && tutoring.getEndTime().isBefore(currentTime)) {
                                        log.info("NOVA LÓGICA (Filtro Tempo): Mentoria concreta ID {} (Status: {}) descartada pois o horário de término {} já passou (horário atual: {}).",
                                                        tutoring.getId(), tutoring.getStatus(), tutoring.getEndTime(),
                                                        currentTime);
                                        include = false;
                                }
                        }
                        if (include) {
                                validConcreteTutorings.add(tutoring);
                        }
                }
                log.info("NOVA LÓGICA (Filtro Tempo): {} mentorias concretas restantes após filtro de horário.",
                                validConcreteTutorings.size());

                // Adiciona mentorias concretas válidas à lista de resultados
                for (Tutoring tutoring : validConcreteTutorings) {
                        TutoringRepresentation rep = enrichTutoringRepresentation(tutoring);
                        if (rep != null) {
                                log.info("NOVA LÓGICA: Adicionando mentoria concreta ID {} (Status: {}) à lista de resultados.",
                                                rep.id(), rep.status());
                                resultSlots.add(rep);
                        }
                }

                // 2. Gerar chaves para TODAS essas mentorias concretas VÁLIDAS (para lógica de
                // substituição de disponibilidades)
                Set<String> concreteTutoringKeysToSupersedeAvailability = generateConcreteTutoringKeys(
                                validConcreteTutorings, date);
                log.info("NOVA LÓGICA: Geradas {} chaves para mentorias concretas VÁLIDAS, para 'supersede': {}",
                                concreteTutoringKeysToSupersedeAvailability.size(),
                                concreteTutoringKeysToSupersedeAvailability);

                // 3. Processar disponibilidades (slots A_MARCAR)
                List<MentorAvailability> allAvailabilitiesOnDay = findAllAvailabilitiesByDayOfWeek(
                                mapJavaDayOfWeekToDayWeekEnum(date.getDayOfWeek()));
                log.info("NOVA LÓGICA: Encontradas {} disponibilidades totais para o dia da semana {}",
                                allAvailabilitiesOnDay.size(), date.getDayOfWeek());

                allAvailabilitiesOnDay.stream()
                                .filter(avail -> { // Lógica de relevância da disponibilidade
                                        if (!Boolean.TRUE.equals(avail.getIsAvailable()))
                                                return false;
                                        if (avail.getUser() == null || avail.getUser().getId() == null)
                                                return false;
                                        if (avail.getDiscipline() == null || avail.getDiscipline().getId() == null)
                                                return false;

                                        if (disciplineId.isPresent()
                                                        && !avail.getDiscipline().getId().equals(disciplineId.get())) {
                                                return false;
                                        }
                                        // Adiciona filtro de tempo para disponibilidades no dia atual
                                        if (date.equals(currentDate)) {
                                                if (avail.getEndTime() != null
                                                                && avail.getEndTime().isBefore(currentTime)) {
                                                        log.debug("NOVA LÓGICA (Filtro Tempo): Disponibilidade (A_MARCAR) ID {} (Mentor ID {}) descartada pois o horário de término {} já passou (horário atual: {}).",
                                                                        (avail.getId() != null ? avail.getId() : "N/A"),
                                                                        (avail.getUser() != null && avail.getUser()
                                                                                        .getId() != null ? avail
                                                                                                        .getUser()
                                                                                                        .getId()
                                                                                                        : "N/A"),
                                                                        avail.getEndTime(), currentTime);
                                                        return false;
                                                }
                                        }
                                        log.debug("NOVA LÓGICA: Disponibilidade ID {} (Mentor ID {}) é relevante (incluindo filtro de tempo).",
                                                        (avail.getId() != null ? avail.getId() : "N/A"),
                                                        (avail.getUser() != null && avail.getUser().getId() != null
                                                                        ? avail.getUser().getId()
                                                                        : "N/A"));
                                        return true;
                                })
                                .filter(avail -> { // Filtro para não mostrar disponibilidade se já existe mentoria
                                                   // concreta para ela
                                        boolean isSuperseded = isAvailabilitySuperseded(avail,
                                                        concreteTutoringKeysToSupersedeAvailability, date);
                                        log.debug("NOVA LÓGICA: Disponibilidade ID {}. Substituída (Superseded) por mentoria PENDENTE/AGENDADA: {}",
                                                        (avail.getId() != null ? avail.getId() : "N/A"), isSuperseded);
                                        return !isSuperseded; // Só adiciona se NÃO for substituída
                                })
                                .map(avail -> tutoringMapper.availabilityToTutoringRepresentation(avail, date))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .forEach(representation -> {
                                        log.info("NOVA LÓGICA: Adicionando disponibilidade mapeada (A_MARCAR) para Mentor ID {} Disciplina ID {} Data {} {} {}",
                                                        representation.mentorId(), representation.disciplineId(),
                                                        representation.tutoringDate(), representation.startTime(),
                                                        representation.endTime());
                                        resultSlots.add(representation);
                                });

                resultSlots.sort(Comparator.comparing(TutoringRepresentation::startTime,
                                Comparator.nullsLast(String::compareTo)));
                log.info("NOVA LÓGICA: Retornando {} slots no total.", resultSlots.size());
                return resultSlots;
        }

        // --- Métodos de consulta de disponibilidade movidos/implementados aqui ---
        @Override
        @Transactional(readOnly = true)
        public Optional<UserAvailabilityRepresentation> findMentorAvailabilityRepresentationById(Long availabilityId) {
                log.debug("TutoringQueryAdapter: Finding availability representation by ID: {}", availabilityId);
                return mentorAvailabilityRepository.findById(availabilityId)
                                .map(tutoringMapper::toUserAvailabilityRepresentation);
        }

        @Override
        @Transactional(readOnly = true)
        public List<UserAvailabilityRepresentation> findMentorAvailabilitiesByMentorId(Long mentorId) {
                log.debug("TutoringQueryAdapter: Finding availability representations for mentor ID: {}", mentorId);
                // Verifica se o mentor existe antes de buscar suas disponibilidades
                userQuery.findById(mentorId)
                                .orElseThrow(() -> new UserNotFoundException(
                                                "Mentor não encontrado com ID: " + mentorId));
                List<MentorAvailability> availabilities = mentorAvailabilityRepository.findByUserId(mentorId);
                return tutoringMapper.toUserAvailabilityRepresentationList(availabilities);
        }

        @Override
        @Transactional(readOnly = true)
        public Optional<MentorProfileRepresentation> findMentorProfileByMentorId(Long mentorId) {
                log.debug("TutoringQueryAdapter: Finding mentor profile for mentor ID: {}", mentorId);
                return userQuery.findById(mentorId).map(userRep -> { // Usa UserQuery para obter UserRepresentation
                        List<MentorAvailability> mentorAvailabilities = mentorAvailabilityRepository
                                        .findByUserId(mentorId);

                        Map<Discipline, List<MentorAvailability>> groupedByDiscipline = mentorAvailabilities.stream()
                                        .filter(avail -> avail.getDiscipline() != null)
                                        .collect(Collectors.groupingBy(MentorAvailability::getDiscipline));

                        List<MentorDisciplineAvailabilityRepresentation> disciplineAvailabilities = groupedByDiscipline
                                        .entrySet()
                                        .stream()
                                        .map(entry -> tutoringMapper.toMentorDisciplineAvailabilityRepresentation(
                                                        entry.getKey(),
                                                        entry.getValue()))
                                        .sorted(Comparator.comparing(
                                                        MentorDisciplineAvailabilityRepresentation::disciplineName,
                                                        String.CASE_INSENSITIVE_ORDER))
                                        .collect(Collectors.toList());

                        return new MentorProfileRepresentation(
                                        userRep.id(),
                                        userRep.firstName(),
                                        userRep.lastName(),
                                        disciplineAvailabilities);
                });
        }

        @Override
        @Transactional(readOnly = true)
        public List<MentorProfileRepresentation> findAllMentorProfiles() {
                log.debug("TutoringQueryAdapter: Finding all mentor profiles.");
                List<Long> mentorUserIds = mentorAvailabilityRepository.findAll().stream()
                                .map(MentorAvailability::getUser)
                                .filter(Objects::nonNull)
                                .map(User::getId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .collect(Collectors.toList());
                if (mentorUserIds.isEmpty()) {
                        return Collections.emptyList();
                }

                // Busca UserRepresentation para cada mentorId
                Map<Long, UserRepresentation> userRepMap = new HashMap<>();
                mentorUserIds.forEach(id -> userQuery.findById(id).ifPresent(rep -> userRepMap.put(id, rep)));

                List<MentorAvailability> allAvailabilities = mentorAvailabilityRepository.findByUserIdIn(mentorUserIds);
                Map<Long, List<MentorAvailability>> availabilitiesByMentorId = allAvailabilities.stream()
                                .filter(avail -> avail.getUser() != null)
                                .collect(Collectors.groupingBy(avail -> avail.getUser().getId()));

                return mentorUserIds.stream()
                                .map(id -> {
                                        UserRepresentation userRep = userRepMap.get(id);
                                        if (userRep == null)
                                                return null;

                                        List<MentorAvailability> mentorAvs = availabilitiesByMentorId.getOrDefault(id,
                                                        Collections.emptyList());
                                        Map<Discipline, List<MentorAvailability>> groupedByDiscipline = mentorAvs
                                                        .stream()
                                                        .filter(avail -> avail.getDiscipline() != null)
                                                        .collect(Collectors
                                                                        .groupingBy(MentorAvailability::getDiscipline));

                                        List<MentorDisciplineAvailabilityRepresentation> disciplineAvs = groupedByDiscipline
                                                        .entrySet()
                                                        .stream()
                                                        .map(entry -> tutoringMapper
                                                                        .toMentorDisciplineAvailabilityRepresentation(
                                                                                        entry.getKey(),
                                                                                        entry.getValue()))
                                                        .sorted(Comparator.comparing(
                                                                        MentorDisciplineAvailabilityRepresentation::disciplineName,
                                                                        String.CASE_INSENSITIVE_ORDER))
                                                        .collect(Collectors.toList());

                                        return new MentorProfileRepresentation(
                                                        userRep.id(), userRep.firstName(), userRep.lastName(),
                                                        disciplineAvs);
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<TutoringRepresentation> getUserMentoringSessions(Long userId) {
                log.debug("Adapter: Getting mentoring sessions for user ID (as mentor): {}", userId);
                if (this.tutoringRepository == null) { // Ponto de atenção
                        log.error("TutoringQuery is not injected in UserQueryAdapter for getUserMentoringSessions");
                        throw new IllegalStateException("TutoringQuery service not available");
                }
                if (!userQuery.findById(userId).isPresent()) {
                        log.warn("Adapter: User not found with ID: {}", userId);
                        throw new UserNotFoundException("Usuário não encontrado com ID: " + userId);
                }
                return this.findAllTutoringsByMentorId(userId);
        }

        @Override
        @Transactional(readOnly = true)
        public List<TutoringParticipationRepresentation> getUserParticipationSessions(Long userId) {
                log.debug(
                                "Adapter: Obtendo sessões de participação para o usuário ID: {} usando TutoringParticipationRepresentation",
                                userId);

                // Verifica se o usuário existe primeiro
                if (!userQuery.findById(userId).isPresent()) {
                        log.warn("Adapter: Usuário com ID: {} não encontrado ao buscar sessões de participação.",
                                        userId);
                        throw new UserNotFoundException("Usuário não encontrado com ID: " + userId);
                }

                List<TutoringParticipants> participations = tutoringParticipantsRepository.findByUserId(userId);

                if (participations.isEmpty()) {
                        log.debug("Adapter: Nenhuma sessão de participação encontrada para o usuário ID: {}", userId);
                        return Collections.emptyList();
                }

                return participations.stream()
                                .map(TutoringParticipants::getTutoring)
                                .filter(Objects::nonNull)
                                .distinct()
                                .map(tutoringMapper::toTutoringParticipationRepresentation)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<MentorAvailability> findAllAvailabilitiesByDayOfWeek(DayWeek dayOfWeek) {
                log.debug("TutoringQueryAdapter (as UserAvailabilityFinder): Finding all mentor availabilities for day: {}",
                                dayOfWeek);
                return mentorAvailabilityRepository.findByDayOfWeek(dayOfWeek);
        }

        private DayWeek mapJavaDayOfWeekToDayWeekEnum(java.time.DayOfWeek javaDayOfWeek) {
                if (javaDayOfWeek == null) {
                        throw new IllegalArgumentException("java.time.DayOfWeek não pode ser nulo para mapeamento.");
                }
                switch (javaDayOfWeek) {
                        case MONDAY:
                                return DayWeek.SEGUNDA_FEIRA;
                        case TUESDAY:
                                return DayWeek.TERCA_FEIRA;
                        case WEDNESDAY:
                                return DayWeek.QUARTA_FEIRA;
                        case THURSDAY:
                                return DayWeek.QUINTA_FEIRA;
                        case FRIDAY:
                                return DayWeek.SEXTA_FEIRA;
                        case SATURDAY:
                                return DayWeek.SABADO;
                        case SUNDAY:
                                return DayWeek.DOMINGO;
                        default:
                                throw new IllegalArgumentException(
                                                "Dia da semana não mapeado: " + javaDayOfWeek.name());
                }
        }

        private boolean isTutoringAvailableForUser(Tutoring tutoring, Long requestingUserId) {
                if (tutoring == null || requestingUserId == null) {
                        log.warn("isTutoringAvailableForUser: Tutoring ou requestingUserId é nulo. Tutoring: {}, UserID: {}",
                                        tutoring, requestingUserId);
                        return false;
                }

                // Permitir PENDENTE e AGENDADA como status válidos para um slot "disponível
                // para entrar"
                if (tutoring.getStatus() != StatusTutoring.AGENDADA
                                && tutoring.getStatus() != StatusTutoring.PENDENTE) {
                        log.debug("isTutoringAvailableForUser: Tutoria ID {} com status {} não é AGENDADA nem PENDENTE. Retornando false.",
                                        (tutoring.getId() != null ? tutoring.getId() : "N/A"), tutoring.getStatus());
                        return false;
                }

                if (tutoring.getMentor() != null && tutoring.getMentor().getId().equals(requestingUserId)) {
                        log.debug("isTutoringAvailableForUser: Usuário {} é o mentor da tutoria ID {}. Retornando false.",
                                        requestingUserId, (tutoring.getId() != null ? tutoring.getId() : "N/A"));
                        return false;
                }

                long currentParticipants = tutoringParticipantsRepository.countByTutoringId(tutoring.getId());
                boolean isFull = tutoring.getMaxParticipants() != null
                                && currentParticipants >= tutoring.getMaxParticipants();

                if (isFull) {
                        log.debug("isTutoringAvailableForUser: Tutoria ID {} está lotada (Max: {}, Atual: {}). Retornando false.",
                                        (tutoring.getId() != null ? tutoring.getId() : "N/A"),
                                        tutoring.getMaxParticipants(), currentParticipants);
                        return false;
                }

                boolean alreadyParticipant = tutoringParticipantsRepository
                                .existsByTutoringIdAndUserId(tutoring.getId(), requestingUserId);
                if (alreadyParticipant) {
                        log.debug("isTutoringAvailableForUser: Usuário {} já é participante da tutoria ID {}. Retornando false.",
                                        requestingUserId, (tutoring.getId() != null ? tutoring.getId() : "N/A"));
                }
                return !alreadyParticipant;
        }

        private Set<String> generateConcreteTutoringKeys(List<Tutoring> tutorings, LocalDate date) {
                return tutorings.stream()
                                .map(tutoring -> {
                                        Long mentorIdKey = (tutoring.getMentor() != null
                                                        && tutoring.getMentor().getId() != null)
                                                                        ? tutoring.getMentor().getId()
                                                                        : -1L;
                                        Long disciplineIdKey = (tutoring.getDiscipline() != null
                                                        && tutoring.getDiscipline().getId() != null)
                                                                        ? tutoring.getDiscipline().getId()
                                                                        : -1L;
                                        String startTimeStr = tutoring.getStartTime() != null
                                                        ? tutoring.getStartTime().format(TutoringMapper.TIME_FORMATTER)
                                                        : "null";
                                        String endTimeStr = tutoring.getEndTime() != null
                                                        ? tutoring.getEndTime().format(TutoringMapper.TIME_FORMATTER)
                                                        : "null";
                                        String dateStr = date != null ? date.format(TutoringMapper.DATE_FORMATTER)
                                                        : "null-date";
                                        return String.format("%d-%d-%s-%s-%s", mentorIdKey, disciplineIdKey, dateStr,
                                                        startTimeStr,
                                                        endTimeStr);
                                })
                                .collect(Collectors.toSet());
        }

        private boolean isAvailabilityRelevantForSlots(MentorAvailability avail, Long requestingUserId,
                        Optional<Long> disciplineId) {
                if (!Boolean.TRUE.equals(avail.getIsAvailable()))
                        return false;
                if (avail.getUser() == null || avail.getUser().getId() == null)
                        return false;
                if (requestingUserId != null && avail.getUser().getId().equals(requestingUserId))
                        return false;

                if (disciplineId.isPresent()) {
                        if (avail.getDiscipline() == null || avail.getDiscipline().getId() == null ||
                                        !avail.getDiscipline().getId().equals(disciplineId.get())) {
                                return false;
                        }
                }
                if (avail.getStartTime() == null || avail.getEndTime() == null) {
                        log.warn("Disponibilidade ID {} com tempo de início ou fim nulo encontrada.",
                                        (avail.getId() != null ? avail.getId() : "N/A"));
                        return false;
                }
                return true;
        }

        private boolean isAvailabilitySuperseded(MentorAvailability avail, Set<String> concreteTutoringKeys,
                        LocalDate date) {
                Long availMentorId = (avail.getUser() != null && avail.getUser().getId() != null)
                                ? avail.getUser().getId()
                                : -1L;
                Long availDisciplineId = (avail.getDiscipline() != null && avail.getDiscipline().getId() != null)
                                ? avail.getDiscipline().getId()
                                : -1L;
                String availStartTimeStr = avail.getStartTime() != null
                                ? avail.getStartTime().format(TutoringMapper.TIME_FORMATTER)
                                : "null";
                String availEndTimeStr = avail.getEndTime() != null
                                ? avail.getEndTime().format(TutoringMapper.TIME_FORMATTER)
                                : "null";
                String dateStr = date != null ? date.format(TutoringMapper.DATE_FORMATTER) : "null-date";

                String availabilityKey = String.format("%d-%d-%s-%s-%s", availMentorId, availDisciplineId, dateStr,
                                availStartTimeStr, availEndTimeStr);

                boolean isSuperseded = concreteTutoringKeys.contains(availabilityKey);
                if (isSuperseded) {
                        log.trace("TutoringQueryAdapter: Availability (Key: {}) is superseded by a concrete tutoring slot.",
                                        availabilityKey);
                }
                return isSuperseded;
        }

}