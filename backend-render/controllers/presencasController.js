import {
  addPresenca,
  deletePresenca as deletePresencaRecord,
  getPresenca,
  getPresencas,
  getPresencasPorTreino,
  updatePresenca as updatePresencaRecord,
} from '../services/storageService.js';

import { query } from '../services/databaseService.js';

export async function listPresencas(req, res) {
  try {
    const professorId =
      req.usuario.administrador === true
        ? null
        : Number(req.usuario.id);

    const presencas = await getPresencas(professorId);

    return res.json(presencas);
  } catch (error) {
    console.error('Erro ao buscar presenças:', error);

    return res.status(500).json({
      error: 'Erro ao buscar presenças.',
    });
  }
}

export async function listPresencasPorTreino(req, res) {
  try {
    const { treinoId } = req.params;
    const data = req.query.data;

    if (
      !treinoId ||
      !/^[0-9]+$/.test(String(treinoId))
    ) {
      return res.status(400).json({
        error: 'ID do treino inválido.',
      });
    }

    const treinoIdNumero = Number(treinoId);

    const dataStr =
      typeof data === 'string' && data.trim()
        ? data.trim()
        : null;

    if (dataStr) {
      const partes = dataStr.split('-');

      if (
        partes.length !== 3 ||
        !/^\d{4}$/.test(partes[0]) ||
        !/^\d{2}$/.test(partes[1]) ||
        !/^\d{2}$/.test(partes[2])
      ) {
        return res.status(400).json({
          error: 'Data inválida. Use o formato YYYY-MM-DD.',
        });
      }
    }

    const treinoResult = await query(
      `SELECT professor_id FROM public.treinos WHERE id = $1`,
      [treinoIdNumero]
    );

    if (treinoResult.rows.length === 0) {
      return res.status(404).json({
        error: 'Treino não encontrado.',
      });
    }

    if (
      req.usuario.administrador !== true &&
      Number(treinoResult.rows[0].professor_id) !== Number(req.usuario.id)
    ) {
      return res.status(403).json({
        error: 'Acesso negado a este treino.',
      });
    }

    const professorId =
      req.usuario.administrador === true
        ? null
        : Number(req.usuario.id);

    const presencas = await getPresencasPorTreino(
      treinoIdNumero,
      dataStr,
      professorId
    );

    return res.json(presencas);
  } catch (error) {
    console.error('Erro ao buscar presenças por treino:', error);

    return res.status(500).json({
      error: 'Erro ao buscar presenças por treino.',
    });
  }
}

export async function createPresenca(req, res) {
  try {
    const presenca = req.body;

    if (
      !presenca ||
      typeof presenca !== 'object' ||
      Array.isArray(presenca)
    ) {
      return res.status(400).json({
        error: 'Dados de presença inválidos.',
      });
    }

    const alunoId =
      presenca.aluno_id ??
      presenca.alunoId ??
      presenca.aluno;

    const treinoId =
      presenca.treino_id ??
      presenca.treinoId ??
      presenca.treino;

    const data = presenca.data;
    const status = presenca.status;

    const alunoIdNumero = Number(alunoId);
    const treinoIdNumero = Number(treinoId);

    if (
      !Number.isInteger(alunoIdNumero) ||
      alunoIdNumero <= 0
    ) {
      return res.status(400).json({
        error: 'ID do aluno inválido.',
      });
    }

    if (
      !Number.isInteger(treinoIdNumero) ||
      treinoIdNumero <= 0
    ) {
      return res.status(400).json({
        error: 'ID do treino inválido.',
      });
    }

    if (
      typeof data !== 'string' ||
      !data.trim()
    ) {
      return res.status(400).json({
        error: 'Data da presença é obrigatória.',
      });
    }

    const statusPermitido = [
      'presente',
      'falta',
      'justificado',
    ];

    if (
      typeof status !== 'string' ||
      !statusPermitido.includes(status.trim())
    ) {
      return res.status(400).json({
        error: 'Status de presença inválido.',
        permitidos: statusPermitido,
      });
    }

    const treinoResult = await query(
      `SELECT turma_id FROM public.treinos WHERE id = $1`,
      [treinoIdNumero]
    );

    if (treinoResult.rows.length === 0) {
      return res.status(400).json({
        error: 'Treino informado não existe.',
      });
    }

    const turmaIdDoTreino = treinoResult.rows[0].turma_id;

    if (turmaIdDoTreino) {
      const turmaAlunoResult = await query(
        `SELECT 1 FROM public.turma_alunos WHERE turma_id = $1 AND aluno_id = $2`,
        [turmaIdDoTreino, alunoIdNumero]
      );

      if (turmaAlunoResult.rows.length === 0) {
        console.warn('[PRESENCAS] Validação aluno-turma falhou:', {
          aluno_id: alunoIdNumero,
          treino_id: treinoIdNumero,
          turma_id: turmaIdDoTreino,
        });

        return res.status(400).json({
          error:
            'Aluno não pertence à turma associada a este treino.',
        });
      }
    }

    const novaPresenca = await addPresenca({
      aluno_id: alunoIdNumero,
      treino_id: treinoIdNumero,
      data: data.trim(),
      status: status.trim(),
    });

    if (novaPresenca?.status === 'closed') {
      return res.status(409).json({
        error: 'A chamada para este treino e data já foi encerrada.',
      });
    }

    return res.status(201).json(novaPresenca);

  } catch (error) {
    console.error('Erro ao criar presença:', error);

    if (error?.code === '23505') {
      return res.status(409).json({
        error:
          'Já existe uma presença registrada para este aluno, treino e data.',
      });
    }

    if (error?.code === '23503') {
      return res.status(400).json({
        error: 'Aluno ou treino informado não existe.',
      });
    }

    return res.status(500).json({
      error: 'Erro ao criar presença.',
    });
  }
}

export async function updatePresenca(req, res) {
  try {
    const { id } = req.params;

    if (!id || !/^[0-9]+$/.test(id)) {
      return res.status(400).json({
        error: 'ID de presença inválido.',
      });
    }

    const presenca = await getPresenca(id);

    if (!presenca) {
      return res.status(404).json({
        error: 'Presença não encontrada.',
      });
    }

    if (
      req.usuario.administrador !== true &&
      Number(presenca.professor_id) !== Number(req.usuario.id)
    ) {
      return res.status(403).json({
        error: 'Acesso negado a esta presença.',
      });
    }

    const body = req.body;

    if (
      !body ||
      typeof body !== 'object' ||
      Array.isArray(body)
    ) {
      return res.status(400).json({
        error: 'Dados de presença inválidos.',
      });
    }

    const dadosAtualizados = {};

    if (body.status !== undefined) {
      const statusPermitido = [
        'presente',
        'falta',
        'justificado',
      ];

      if (
        typeof body.status !== 'string' ||
        !statusPermitido.includes(body.status.trim())
      ) {
        return res.status(400).json({
          error: 'Status de presença inválido.',
          permitidos: statusPermitido,
        });
      }

      dadosAtualizados.status = body.status.trim();
    }

    if (body.data !== undefined) {
      if (
        typeof body.data !== 'string' ||
        !body.data.trim()
      ) {
        return res.status(400).json({
          error: 'Data da presença é obrigatória.',
        });
      }

      dadosAtualizados.data = body.data.trim();
    }

    if (!Object.keys(dadosAtualizados).length) {
      return res.status(400).json({
        error: 'Nenhum campo válido para atualizar.',
      });
    }

    const atualizada = await updatePresencaRecord(
      id,
      dadosAtualizados
    );

    if (!atualizada || atualizada.status === 'not_found') {
      return res.status(404).json({
        error: 'Presença não encontrada.',
      });
    }

    if (atualizada.status === 'closed') {
      return res.status(409).json({
        error: 'A chamada para este treino e data já foi encerrada.',
      });
    }

    return res.json(atualizada.presenca || atualizada);
  } catch (error) {
    console.error('Erro ao atualizar presença:', error);

    return res.status(500).json({
      error: 'Erro ao atualizar presença.',
    });
  }
}

export async function deletePresenca(req, res) {
  try {
    const { id } = req.params;

    if (!id || !/^[0-9]+$/.test(id)) {
      return res.status(400).json({
        error: 'ID de presença inválido.',
      });
    }

    const presenca = await getPresenca(id);

    if (!presenca) {
      return res.status(404).json({
        error: 'Presença não encontrada.',
      });
    }

    if (
      req.usuario.administrador !== true &&
      Number(presenca.professor_id) !== Number(req.usuario.id)
    ) {
      return res.status(403).json({
        error: 'Acesso negado a esta presença.',
      });
    }

    const excluida = await deletePresencaRecord(id);

    if (!excluida || excluida.status === 'not_found') {
      return res.status(404).json({
        error: 'Presença não encontrada.',
      });
    }

    if (excluida.status === 'closed') {
      return res.status(409).json({
        error: 'A chamada para este treino e data já foi encerrada.',
      });
    }

    return res.status(204).send();
  } catch (error) {
    console.error('Erro ao excluir presença:', error);

    return res.status(500).json({
      error: 'Erro ao excluir presença.',
    });
  }
}
