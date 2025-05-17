// src/main/java/com/projetointegrador/seumentor/tutoring/api/mapper/TutoringMapper.java
package com.projetointegrador.seumentor.tutoring.api.mapper;

import com.projetointegrador.seumentor.common.enums.DayWeek;
import com.projetointegrador.seumentor.course.model.CourseArea;
import com.projetointegrador.seumentor.tutoring.model.Tutoring;
import com.projetointegrador.seumentor.tutoring.model.MentorAvailability;
import com.projetointegrador.seumentor.tutoring.model.TutoringClassType;
import com.projetointegrador.seumentor.tutoring.model.TutoringParticipants;
import com.projetointegrador.seumentor.tutoring.model.TutoringRating;
import com.projetointegrador.seumentor.tutoring.api.dto.*; // Importa todos os DTOs de tutoring.api.dto
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.user.model.User;
import com.projetointegrador.seumentor.tutoring.enums.StatusTutoring;

import org.mapstruct.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.Optional; // Para o retorno de availabilityToTutoringRepresentation
import java.util.Collections; // Para Collections.emptySet()
import java.util.stream.Collectors;
import java.util.ArrayList;

@Mapper(componentModel = "spring",
        imports = {DateTimeFormatter.class, Collectors.class, TutoringClassType.class, StatusTutoring.class, User.class, Discipline.class, ArrayList.class, LoggerFactory.class, Collections.class, Optional.class, CourseArea.class, DayWeek.class},
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public abstract class TutoringMapper {

    private static final Logger log = LoggerFactory.getLogger(TutoringMapper.class);

    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // --- Mapeamento para ScheduledTutoringRepresentation ---
    @Mapping(target = "mentorName", expression = "java(userToFullName(tutoring.getMentor()))")
    @Mapping(target = "mentorId", source = "mentor.id")
    @Mapping(target = "disciplineName", expression = "java(disciplineToName(tutoring.getDiscipline()))")
    @Mapping(target = "disciplineId", source = "discipline.id")
    @Mapping(target = "startTime", source = "startTime", qualifiedByName = "formatTimeSafe")
    @Mapping(target = "endTime", source = "endTime", qualifiedByName = "formatTimeSafe")
    @Mapping(target = "tutoringDate", source = "tutoringDate", qualifiedByName = "formatDateSafe")
    @Mapping(target = "topics", expression = "java(mapTopicsToStringList(tutoring.getTopics()))")
    @Mapping(target = "local", source = "tutoring", qualifiedByName = "determineLocalValueScheduled")
    @Mapping(target = "linkVideo", source = "tutoring", qualifiedByName = "determineLinkVideoValueScheduled")
    @Mapping(target = "qtdParticipants", ignore = true)
    public abstract ScheduledTutoringRepresentation toScheduledTutoringRepresentation(Tutoring tutoring);

    // --- Mapeamento para TutoringRepresentation ---
    @Mapping(target = "mentorName", expression = "java(userToFullName(tutoring.getMentor()))")
    @Mapping(target = "mentorId", source = "mentor.id")
    @Mapping(target = "disciplineName", expression = "java(disciplineToName(tutoring.getDiscipline()))")
    @Mapping(target = "disciplineId", source = "discipline.id")
    @Mapping(target = "startTime", source = "startTime", qualifiedByName = "formatTimeSafe")
    @Mapping(target = "endTime", source = "endTime", qualifiedByName = "formatTimeSafe")
    @Mapping(target = "tutoringDate", source = "tutoringDate", qualifiedByName = "formatDateSafe")
    @Mapping(target = "participants", source = "topics", qualifiedByName = "mapTopicsToParticipantInfoSet")
    @Mapping(target = "local", source = "tutoring", qualifiedByName = "determineLocalValueGeneral")
    @Mapping(target = "linkVideo", source = "tutoring", qualifiedByName = "determineLinkVideoValueGeneral")
    @Mapping(target = "qtdParticipants", ignore = true)
    public abstract TutoringRepresentation toTutoringRepresentation(Tutoring tutoring);

    // --- Mapeamento para TutoringParticipationRepresentation ---
    @Mapping(target = "mentorName", expression = "java(userToFullName(tutoring.getMentor()))")
    @Mapping(target = "mentorId", source = "mentor.id")
    @Mapping(target = "disciplineName", expression = "java(disciplineToName(tutoring.getDiscipline()))")
    @Mapping(target = "disciplineId", source = "discipline.id")
    @Mapping(target = "startTime", source = "startTime", qualifiedByName = "formatTimeSafe")
    @Mapping(target = "endTime", source = "endTime", qualifiedByName = "formatTimeSafe")
    @Mapping(target = "tutoringDate", source = "tutoringDate", qualifiedByName = "formatDateSafe")
    @Mapping(target = "topics", expression = "java(mapTopicsToStringList(tutoring.getTopics()))")
    @Mapping(target = "local", source = "tutoring", qualifiedByName = "determineLocalValueGeneral")
    @Mapping(target = "linkVideo", source = "tutoring", qualifiedByName = "determineLinkVideoValueGeneral")
    @Mapping(target = "qtdParticipants", ignore = true)
    public abstract TutoringParticipationRepresentation toTutoringParticipationRepresentation(Tutoring tutoring);


    // --- Mapeamento para UserAvailabilityRepresentation ---
    @Mapping(target = "discipline", source = "discipline", qualifiedByName = "disciplineToSimpleDisciplineRepSafe")
    public abstract UserAvailabilityRepresentation toUserAvailabilityRepresentation(MentorAvailability availability);

    public abstract List<UserAvailabilityRepresentation> toUserAvailabilityRepresentationList(List<MentorAvailability> availabilities);

    // --- Mapeamento para TutoringRatingRepresentation ---
    @Mapping(target = "tutoringId", source = "rating.tutoring.id")
    public abstract TutoringRatingRepresentation toTutoringRatingRepresentation(TutoringRating rating, @Context Long raterUserId);
    // Adicionado @Context para raterUserId se ele não for um campo direto de TutoringRating
    // Se raterUserId não for usado para mapear nenhum campo em TutoringRatingRepresentation, pode ser removido do método.
    // Assumindo que o DTO tem um campo para raterUserId que será preenchido diretamente.
    // Se `TutoringRatingRepresentation` tem um campo `ratedByUserId`, e `raterUserId` é para ele:
    // @Mapping(target = "ratedByUserId", source = "raterUserId") -> se raterUserId é o nome do parâmetro


    // --- Métodos Qualificadores e Auxiliares ---

    @Named("determineLocalValueScheduled")
    protected String determineLocalValueScheduled(Tutoring tutoring) {
        if (tutoring == null) return null;
        if (tutoring.getStatus() == StatusTutoring.PENDENTE) {
            return (tutoring.getTutoringClassType() == TutoringClassType.PRESENCIAL) ? "A definir" : "Não se aplica";
        }
        if (tutoring.getTutoringClassType() == TutoringClassType.PRESENCIAL) {
            return tutoring.getLocal();
        }
        return "Não se aplica";
    }

    @Named("determineLinkVideoValueScheduled")
    protected String determineLinkVideoValueScheduled(Tutoring tutoring) {
        if (tutoring == null) return null;
        if (tutoring.getStatus() == StatusTutoring.PENDENTE) {
            return (tutoring.getTutoringClassType() == TutoringClassType.ONLINE) ? "A definir" : "Não se aplica";
        }
        if (tutoring.getTutoringClassType() == TutoringClassType.ONLINE) {
            return tutoring.getLinkVideo();
        }
        return "Não se aplica";
    }
    
    @Named("determineLocalValueGeneral")
    protected String determineLocalValueGeneral(Tutoring tutoring) {
        if (tutoring == null) return null;
        if (tutoring.getTutoringClassType() == TutoringClassType.PRESENCIAL) {
            return tutoring.getLocal();
        }
        return "Não se aplica";
    }

    @Named("determineLinkVideoValueGeneral")
    protected String determineLinkVideoValueGeneral(Tutoring tutoring) {
        if (tutoring == null) return null;
        if (tutoring.getTutoringClassType() == TutoringClassType.ONLINE) {
            return tutoring.getLinkVideo();
        }
        return "Não se aplica";
    }


    @Named("userToFullName")
    protected String userToFullName(User user) {
        if (user == null) return "[Usuário N/A]"; // Ajustado para ser mais genérico
        try {
            // Assegurar que o objeto User não é um proxy não inicializado
            if (user.getFirstName() == null && user.getId() != null) { // Heurística para proxy
                 log.warn("Tentando acessar nome de um proxy de User não totalmente carregado. ID: {}", user.getId());
                 return "[Usuário Carregando...]";
            }
            return user.getFirstName() + " " + user.getLastName();
        } catch (Exception e) {
            log.warn("Não foi possível obter o nome completo do usuário ID: {}", (user.getId() != null ? user.getId() : "null"), e);
            return "[Usuário Inválido]";
        }
    }

    @Named("disciplineToName")
    protected String disciplineToName(Discipline discipline) {
        if (discipline == null) return "[Disciplina N/A]";
        try {
            if (discipline.getDisciplineName() == null && discipline.getId() != null) { // Heurística para proxy
                log.warn("Tentando acessar nome de um proxy de Discipline não totalmente carregado. ID: {}", discipline.getId());
                return "[Disciplina Carregando...]";
            }
            return discipline.getDisciplineName();
        } catch (Exception e) {
            log.warn("Não foi possível obter o nome da disciplina ID: {}", (discipline.getId() != null ? discipline.getId() : "null"), e);
            return "[Disciplina Inválida]";
        }
    }

    @Named("formatTimeSafe")
    protected String formatTimeSafe(LocalTime time) {
        return (time != null) ? time.format(TIME_FORMATTER) : null;
    }

    @Named("formatDateSafe")
    protected String formatDateSafe(LocalDate date) {
        return (date != null) ? date.format(DATE_FORMATTER) : null;
    }

    @Named("mapTopicsToStringList")
    protected List<String> mapTopicsToStringList(Set<TutoringParticipants> participants) {
        if (participants == null || participants.isEmpty()) {
            return new ArrayList<>();
        }
        return participants.stream()
                           .map(TutoringParticipants::getTopic)
                           .collect(Collectors.toList());
    }

    @Named("mapTopicsToParticipantInfoSet")
    protected Set<TutoringParticipantInfo> mapTopicsToParticipantInfoSet(Set<TutoringParticipants> participants) {
        if (participants == null || participants.isEmpty()) {
            return Collections.emptySet();
        }
        return participants.stream()
                           .map(p -> new TutoringParticipantInfo(
                               p.getUser() != null ? p.getUser().getId() : null,
                               p.getUser() != null ? userToFullName(p.getUser()) : "[Participante N/A]",
                               p.getTopic()
                           ))
                           .collect(Collectors.toSet());
    }

    @Named("disciplineToSimpleDisciplineRepSafe")
    protected SimpleDisciplineRepresentation disciplineToSimpleDisciplineRepSafe(Discipline discipline) {
        if (discipline == null) {
            return new SimpleDisciplineRepresentation(null, "[Disciplina Não Definida]");
        }
        try {
            Long id = discipline.getId();
            String name = discipline.getDisciplineName();
            return new SimpleDisciplineRepresentation(id, name);
        } catch (jakarta.persistence.EntityNotFoundException | org.hibernate.ObjectNotFoundException e) {
            log.warn("Disciplina (proxy) não encontrada ao mapear para SimpleDisciplineRepresentation. ID pode ser {}.", (discipline.getId() != null ? discipline.getId() : "desconhecido"), e);
            return new SimpleDisciplineRepresentation(null, "[Disciplina Inválida/Removida]");
        } catch (Exception e) {
            log.warn("Erro ao acessar dados da disciplina ao mapear para SimpleDisciplineRepresentation. ID pode ser {}.", (discipline.getId() != null ? discipline.getId() : "desconhecido"), e);
            return new SimpleDisciplineRepresentation(null, "[Erro ao Carregar Disciplina]");
        }
    }

    // Para (MentorAvailability, LocalDate) -> Optional<TutoringRepresentation>
    @Named("availabilityToTutoringRepresentation")
    public Optional<TutoringRepresentation> availabilityToTutoringRepresentation(MentorAvailability availability, LocalDate date) {
        if (availability == null || !Boolean.TRUE.equals(availability.getIsAvailable())) {
            return Optional.empty();
        }
        User mentor = availability.getUser();
        Discipline discipline = availability.getDiscipline();

        if (mentor == null || discipline == null || date == null) {
            log.warn("Skipping mapping availability ID {} to TutoringRepresentation due to null User, Discipline or Date.", (availability.getId() != null ? availability.getId() : "N/A"));
            return Optional.empty();
        }
        
        // É importante que userToFullName e disciplineToName lidem com proxies
        String mentorFullName = userToFullName(mentor);
        String discName = disciplineToName(discipline);

        return Optional.of(new TutoringRepresentation(
                null, // No ID for an availability slot
                mentor.getId(),
                mentorFullName,
                discipline.getId(),
                discName,
                availability.getTutoringClassType(),
                StatusTutoring.A_MARCAR,
                formatTimeSafe(availability.getStartTime()),
                formatTimeSafe(availability.getEndTime()),
                formatDateSafe(date),
                null, 
                null, 
                null, 
                0,    
                false, 
                Collections.emptySet()
        ));
    }

    // Para (Discipline, List<MentorAvailability>) -> MentorDisciplineAvailabilityRepresentation
    @Named("toMentorDisciplineAvailabilityRep")
    public MentorDisciplineAvailabilityRepresentation toMentorDisciplineAvailabilityRepresentation(Discipline discipline, List<MentorAvailability> availabilities) {
        if (discipline == null) return null;
        
        String courseName = "[Curso não definido]";
        CourseArea courseArea = null;
        try {
             courseArea = discipline.getCourseArea();
            if (courseArea != null && courseArea.getCourse() != null) {
                 courseName = courseArea.getCourse();
            }
        } catch (Exception e) {
             log.warn("Erro ao acessar CourseArea da disciplina ID {}", discipline.getId(), e);
        }
        
        List<AvailabilitySlotRepresentation> slots = Collections.emptyList();
        if (availabilities != null) {
            slots = availabilities.stream()
                .map(this::toAvailabilitySlotRepresentationInternal) // Chama o método interno
                .collect(Collectors.toList());
        }
        
        return new MentorDisciplineAvailabilityRepresentation(disciplineToName(discipline), courseName, slots);
    }

    // Renomeado para evitar conflito de nome se um método abstrato fosse gerado com a mesma assinatura
    protected AvailabilitySlotRepresentation toAvailabilitySlotRepresentationInternal(MentorAvailability availability) {
        if (availability == null) return null;
        return new AvailabilitySlotRepresentation(
            availability.getDayOfWeek(),
            availability.getStartTime(),
            availability.getEndTime()
        );
    }
}