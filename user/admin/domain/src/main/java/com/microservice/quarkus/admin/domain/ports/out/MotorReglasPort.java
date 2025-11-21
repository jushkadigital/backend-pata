package com.microservice.quarkus.admin.domain.ports.out;

import java.util.Map;

public interface MotorReglasPort {
  /**
   * Evalúa una regla de negocio.
   * 
   * @param nombreRegla El ID único de la regla en tu BD (ej: "AsignarRolesAdmin")
   * @param contexto    Mapa con los inputs (ej: {"AdminType": "SUPER_USER"})
   * @return Mapa con los outputs (ej: {"Roles": ["admin", "write"]})
   */
  Map<String, Object> evaluar(String nombreRegla, Map<String, Object> contexto);
}
