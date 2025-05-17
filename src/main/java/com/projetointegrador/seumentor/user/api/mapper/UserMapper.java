package com.projetointegrador.seumentor.user.api.mapper;

import com.projetointegrador.seumentor.common.util.CPFUtils;
import com.projetointegrador.seumentor.user.model.User;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring", imports = {CPFUtils.class})
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(source = "user.cpf", target = "cpf", qualifiedByName = "formatCpf")
    UserRepresentation toRepresentation(User user);

    List<UserRepresentation> toRepresentationList(List<User> users);

    @Named("formatCpf")
    default String formatCpf(String cpfValue) {
        if (cpfValue == null) {
            return null;
        }
        return CPFUtils.formatar(cpfValue);
    }
}
