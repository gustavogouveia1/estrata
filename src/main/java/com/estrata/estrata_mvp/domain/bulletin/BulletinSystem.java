package com.estrata.estrata_mvp.domain.bulletin;

import com.estrata.estrata_mvp.domain.Project;
import com.estrata.estrata_mvp.domain.TechnicalBulletin;
import com.estrata.estrata_mvp.domain.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;

// --- MODELOS ESPECÍFICOS DE BOLETINS ---

@Entity
@Table(name = "bulletins_spt")
@Getter @Setter
class SptBulletin extends TechnicalBulletin {
    private Double initialDepth;
    private Double finalDepth;
    private Integer blowsFirst30cm;
    private Integer blowsLast30cm;
    private String soilClassification;

    @Override
    public String getBulletinType() { return "SPT"; }
}

@Entity
@Table(name = "bulletins_resistivity")
@Getter @Setter
class ResistivityBulletin extends TechnicalBulletin {
    private String equipmentModel;
    private String method; // Ex: Wenner, Schlumberger
    private Double spacing;
    private Double resistanceValue;

    @Override
    public String getBulletinType() { return "RESISTIVITY"; }
}

// --- DTOs (Data Transfer Objects) ---
class BulletinRequestDTO {
    public String type; // "SPT" ou "RESISTIVITY"
    public Long projectId;
    // Campos comuns e específicos viriam aqui (Map<String, Object> data)
}

// --- LOGICA DE GERAÇÃO (STRATEGY PATTERN) ---

/**
 * Interface que define o contrato para processadores de boletins.
 * Facilita a adição de novos tipos (ex: Sondagem Rotativa) sem quebrar código.
 */
interface BulletinProcessor {
    boolean supports(String type);
    TechnicalBulletin createEntity(BulletinRequestDTO dto, Project project, User author);
    byte[] generatePdf(TechnicalBulletin bulletin);
}

@Service
class SptProcessor implements BulletinProcessor {
    @Override
    public boolean supports(String type) { return "SPT".equalsIgnoreCase(type); }

    @Override
    public TechnicalBulletin createEntity(BulletinRequestDTO dto, Project project, User author) {
        SptBulletin spt = new SptBulletin();
        spt.setProject(project);
        spt.setAuthor(author);
        // Mapear campos do DTO para a Entidade...
        spt.setSoilClassification("Solo Simulado");
        return spt;
    }

    @Override
    public byte[] generatePdf(TechnicalBulletin bulletin) {
        SptBulletin spt = (SptBulletin) bulletin;
        // Simulação de geração de PDF (iText ou OpenPDF)
        String content = "RELATÓRIO DE SONDAGEM SPT\n" +
                "Projeto: " + spt.getProject().getName() + "\n" +
                "Solo: " + spt.getSoilClassification();
        return content.getBytes(); // Retorna bytes do PDF real
    }
}

@Service
class ResistivityProcessor implements BulletinProcessor {
    @Override
    public boolean supports(String type) { return "RESISTIVITY".equalsIgnoreCase(type); }

    @Override
    public TechnicalBulletin createEntity(BulletinRequestDTO dto, Project project, User author) {
        ResistivityBulletin res = new ResistivityBulletin();
        res.setProject(project);
        res.setAuthor(author);
        res.setMethod("Wenner");
        return res;
    }

    @Override
    public byte[] generatePdf(TechnicalBulletin bulletin) {
        return "RELATÓRIO DE RESISTIVIDADE ELÉTRICA...".getBytes();
    }
}

// --- SERVICE FACTORY ---

@Service
class BulletinService {

    private final java.util.List<BulletinProcessor> processors;
    // Injeção de Repositories (ProjectRepository, BulletinRepository) viria aqui

    public BulletinService(java.util.List<BulletinProcessor> processors) {
        this.processors = processors;
    }

    public TechnicalBulletin createBulletin(BulletinRequestDTO dto, User author) {
        // 1. Buscar Projeto (mock)
        Project project = new Project(); // projectRepo.findById(dto.projectId)...

        // 2. Encontrar o processador correto
        BulletinProcessor processor = processors.stream()
                .filter(p -> p.supports(dto.type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de boletim não suportado: " + dto.type));

        // 3. Criar entidade e salvar (simulado)
        TechnicalBulletin bulletin = processor.createEntity(dto, project, author);
        // bulletinRepo.save(bulletin);

        return bulletin;
    }

    public byte[] getBulletinPdf(Long bulletinId) {
        // 1. Buscar Bulletin (mock)
        // TechnicalBulletin bulletin = repo.findById(bulletinId)...
        TechnicalBulletin bulletin = new SptBulletin(); // Stub

        // 2. Processar PDF
        return processors.stream()
                .filter(p -> p.supports(bulletin.getBulletinType()))
                .findFirst()
                .orElseThrow()
                .generatePdf(bulletin);
    }
}