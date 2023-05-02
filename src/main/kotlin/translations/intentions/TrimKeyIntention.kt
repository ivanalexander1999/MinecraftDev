/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.translations.intentions

import com.demonwav.mcdev.translations.TranslationFiles
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException

class TrimKeyIntention : PsiElementBaseIntentionAction() {
    override fun getText() = "Trim translation key"

    override fun getFamilyName() = "Minecraft"

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val translation = TranslationFiles.toTranslation(
            TranslationFiles.seekTranslation(element) ?: return false,
        ) ?: return false

        return translation.key != translation.trimmedKey
    }

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val entry = TranslationFiles.seekTranslation(element) ?: return
        if (!FileModificationService.getInstance().preparePsiElementForWrite(entry)) {
            return
        }

        val translation = TranslationFiles.toTranslation(entry) ?: return
        entry.setName(translation.trimmedKey)
    }
}
