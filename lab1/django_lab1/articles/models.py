from django.db import models
from django.contrib.auth.models import User


class Article(models.Model):
    title = models.CharField(
        max_length=200,
        verbose_name='Заголовок'
    )

    text = models.TextField(
        verbose_name='Текст'
    )

    publication_date = models.DateTimeField(
        verbose_name='Дата публікації'
    )

    author = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        verbose_name='Автор',
        related_name='articles'
    )

    def __str__(self):
        return self.title

    class Meta:
        verbose_name = 'Стаття'
        verbose_name_plural = 'Статті'
        ordering = ['-publication_date']